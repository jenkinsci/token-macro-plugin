package org.jenkinsci.plugins.tokenmacro;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.transform.BeginningOrEndMatchTransorm;
import org.jenkinsci.plugins.tokenmacro.transform.ContentLengthTransform;
import org.jenkinsci.plugins.tokenmacro.transform.SubstringTransform;

import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by acearl on 3/6/2016.
 */
public class Parser {

    private static final int MAX_RECURSION_LEVEL = 10;

    private Stack<Transform> transforms = new Stack<>();
    private StringBuilder output;

    private Run<?, ?> run;
    private FilePath workspace;
    private TaskListener listener;
    private boolean throwException;
    private String stringWithMacro;
    private int recursionLevel;
    private List<TokenMacro> privateTokens;
    private Stack<String> argInfoStack = new Stack<>();
    private int tokenStartIndex;

    private String tokenName;
    private ListMultimap<String,String> args;

    public Parser(Run<?,?> run, FilePath workspace, TaskListener listener, String stringWithMacro, boolean throwException) {
        this.run = run;
        this.workspace = workspace;
        this.listener = listener;
        this.stringWithMacro = stringWithMacro;
        this.output = new StringBuilder();
        this.throwException = throwException;
        this.recursionLevel = 0;
    }

    public Parser(Run<?,?> run, FilePath workspace, TaskListener listener, String stringWithMacro, boolean throwException, int recursionLevel) {
        this.run = run;
        this.workspace = workspace;
        this.listener = listener;
        this.stringWithMacro = stringWithMacro;
        this.output = new StringBuilder();
        this.throwException = throwException;
        this.recursionLevel = recursionLevel;
    }

    public static String process(AbstractBuild<?,?> build, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens) throws MacroEvaluationException {
        return process(build,build.getWorkspace(),listener,stringWithMacro,throwException,privateTokens);
    }

    public static String process(Run<?, ?> run, FilePath workspace, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens) throws MacroEvaluationException {
        return process(run, workspace, listener, stringWithMacro, throwException, privateTokens, 0);
    }

    private static String process(Run<?,?> run, FilePath workspace, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens, int recursionLevel) throws MacroEvaluationException {
        if ( StringUtils.isBlank( stringWithMacro ) ) return stringWithMacro;

        Parser p = new Parser(run, workspace, listener, stringWithMacro, throwException);
        p.parse(privateTokens);

        return p.output.toString();
    }

    private void parse(List<TokenMacro> privateTokens) throws MacroEvaluationException {
        this.privateTokens = privateTokens;
        CharacterIterator c = new StringCharacterIterator(stringWithMacro);
        try {
            while (c.current() != CharacterIterator.DONE) {
                if (c.current() == '$') { // some sort of token?
                    tokenStartIndex = c.getIndex();
                    parseToken(c);
                } else {
                    output.append(c.current());
                    c.next();
                }
            }
        } catch(Throwable e) {
            if(e.getCause() instanceof MacroEvaluationException) {
                throw (MacroEvaluationException)e.getCause();
            }
            throw new MacroEvaluationException("Error processing tokens", e);
        }
    }

    private void parseToken(CharacterIterator c) throws MacroEvaluationException, IOException, InterruptedException {
        if(c.current() != '$') {
            throw new MacroEvaluationException("Missing $ in macro usage");
        }

        char last = c.current();
        c.next();
        if(c.current() == '$') {
            parseEscapedToken(c);
        } else if(c.current() == '{') {
            parseDelimitedToken(c);
        } else if(Character.isLetter(c.current()) || c.current() == '_') {
            parseNonDelimitedToken(c);
        } else {
            output.append(last);
            if(c.current() != CharacterIterator.DONE) {
                output.append(c.current());
            }
            c.next();
        }
    }

    private void parseEscapedToken(CharacterIterator c) throws MacroEvaluationException {
        if(c.current() != '$') {
            throw new MacroEvaluationException("Missing $ in escaped macro");
        }

        output.append(c.current());
        c.next();
        if(c.current() == '{') {
            parseEscapedDelimitedToken(c);
        } else {
            parseEscapedNonDelimitedToken(c);
        }
    }

    private void parseDelimitedToken(CharacterIterator c) throws MacroEvaluationException, IOException, InterruptedException {
        if(c.current() != '{') {
            throw new MacroEvaluationException("Missing { in delimited macro");
        }

        char last = c.current();
        c.next();
        if(c.current() == '#') {
            addTransform(new ContentLengthTransform());
            c.next();
        }

        // check for valid identifier start
        if(!Character.isLetter(c.current()) && c.current() != '_') {
            output.append("${");
            output.append(c.current());
            c.next();
            return;
        }

        String token = parseIdentifier(c);
        startToken(token);
        if(c.current() == ':') {
            parseSubstringExpansion(c);
        } else if(c.current() == '#') {
            parseBeginningMatchExpansion(c);
        } else if(c.current() == '%') {
            parseEndingMatchExpansion(c);
        }

        while(Character.isSpaceChar(c.current())) {
            c.next();
        }

        if(c.current() == ',') {
            parseArguments(c);
        }

        if(c.current() != '}') {
            throw new MacroEvaluationException("Missing } in macro usage");
        }

        processToken(c.getIndex());
        c.next();
    }

    private void parseNonDelimitedToken(CharacterIterator c) throws MacroEvaluationException, IOException, InterruptedException {
        String token = parseIdentifier(c);
        if(StringUtils.isNotBlank(token)) {
            startToken(token);
            processToken(c.getIndex());
        }
    }

    private void parseEscapedDelimitedToken(CharacterIterator c) throws MacroEvaluationException {
        if(c.current() != '{') {
            throw new MacroEvaluationException("Missing { in macro");
        }

        while(c.current() != '}') {
            output.append(c.current());
            c.next();
        }
        output.append(c.current());
        c.next();
    }

    private void parseEscapedNonDelimitedToken(CharacterIterator c) throws MacroEvaluationException {
        output.append(parseIdentifier(c));
    }

    private String parseIdentifier(CharacterIterator c) throws MacroEvaluationException {
        StringBuilder builder = new StringBuilder();

        if(Character.isDigit(c.current()) || c.current() == '.' || c.current() == '-') {
            // we have a number
            output.append('$');
            output.append(parseNumericalValue(c));
            return "";
        }

        if(!Character.isLetter(c.current()) && c.current() != '_') {
            throw new MacroEvaluationException("Invalid identifier in macro");
        }

        while(Character.isLetter(c.current()) || Character.isDigit((c.current())) || c.current() == '_') {
            builder.append(c.current());
            c.next();
        }
        return builder.toString();
    }

    /**
     * Rule for substring expansion, which is of the form ${TOKEN:offset:length}, where length is optional. offset and
     * length can be negative, which then operates from the end of the string.
     */
    private void parseSubstringExpansion(CharacterIterator c) throws MacroEvaluationException {
        if(c.current() != ':') {
            throw new MacroEvaluationException("Missing : in substring expansion for macro: " + tokenName);
        }

        boolean isOffsetNegative = false;
        c.next();
        if(c.current() == ' ') {
            // we should have a negative number
            c.next();
            if(c.current() != '-') {
                throw new MacroEvaluationException("Invalid negative offset in substring expansion for macro: " + tokenName);
            }
            isOffsetNegative = true;
            c.next();
        }

        int offset = (isOffsetNegative ? -1 : 1) * Integer.parseInt(parseNumericalValue(c));
        boolean isLengthNegative = false;
        int length = Integer.MAX_VALUE;
        if(c.current() == ':') {
            c.next();
            if(c.current() == '-') {
                isLengthNegative = true;
                c.next();
            }
            length = (isLengthNegative ? -1 : 1) * Integer.parseInt(parseNumericalValue(c));
        }
        addTransform(new SubstringTransform(offset, length));
    }

    /**
     * Rule for beginning match expansion, which is of the form${TOKEN#pattern}, where pattern is a regular expression.
     * Will check for match at the beginning of the string, and if matched remove the matching text.
     */
    private void parseBeginningMatchExpansion(CharacterIterator c) throws MacroEvaluationException {
        if(c.current() != '#') {
            throw new MacroEvaluationException("Missing # in beginning match expansion for macro: " + tokenName);
        }
        c.next();

        String match = parseBeginningEndMatchExpansion(c);
        addTransform(new BeginningOrEndMatchTransorm(match, true));
    }

    private void parseEndingMatchExpansion(CharacterIterator c) throws MacroEvaluationException {
        if(c.current() != '%') {
            throw new MacroEvaluationException("Missing % in ending match expansion for macro: " + tokenName);
        }
        c.next();

        String match = parseBeginningEndMatchExpansion(c);
        addTransform(new BeginningOrEndMatchTransorm(match, false));
    }

    private String parseBeginningEndMatchExpansion(CharacterIterator c) {
        StringBuilder match = new StringBuilder();
        while(true) {
            if(c.current() == '}' || c.current() == ',') {
                break;
            } else if(c.current() == '\\') {
                c.next();
                if (c.current() != '}' && c.current() != ',') {
                    match.append('\\');
                }
                match.append(c.current());
            } else {
                match.append(c.current());
            }
            c.next();
        }
        return match.toString();
    }

    private void parseArguments(CharacterIterator c) throws MacroEvaluationException {
        while(c.current() != '}') {
            if (c.current() != ',') {
                throw new MacroEvaluationException("Missing , for arguments in macro");
            }

            c.next();

            while(Character.isSpaceChar(c.current())) {
                c.next();
            }

            String argName = parseIdentifier(c);
            argInfoStack.push(argName);
            while (Character.isSpaceChar(c.current())) {
                c.next();
            }

            if (c.current() != '=') {
                throw new MacroEvaluationException("Missing = for argument in macro");
            }

            c.next();
            while (Character.isSpaceChar(c.current())) {
                c.next();
            }
            parseArgumentValue(c);
            addArg();

            while(Character.isSpaceChar(c.current())) {
                c.next();
            }
        }
    }

    private void parseArgumentValue(CharacterIterator c) throws MacroEvaluationException {
        if(c.current() == '"') {
            parseStringValue(c);
        } else if(c.current() == 't' || c.current() == 'f' || c.current() == 'T' || c.current() == 'F') {
            parseBooleanValue(c);
        } else {
            argInfoStack.push(parseNumericalValue(c));
        }
    }

    private void parseStringValue(CharacterIterator c) throws MacroEvaluationException {
        StringBuilder builder = new StringBuilder();
        if(c.current() != '"') {
            throw new MacroEvaluationException("Missing \" in argument value for macro: " + tokenName);
        }
        c.next();

        boolean escaped = false;
        while(true) {
            if((c.current() == '\n' || c.current() == '\r') && !escaped) {
                throw new MacroEvaluationException("Newlines are not allowed in string arguments for macro: " + tokenName);
            } else if(c.current() == '\\') {
                escaped = true;
                builder.append(c.current());
                c.next();
            } else if(c.current() == '"' && !escaped) {
                c.next();
                break;
            } else {
                builder.append(c.current());
                c.next();
                escaped = false;
            }
        }

        argInfoStack.push(unescapeString(builder.toString()));
    }

    private void parseBooleanValue(CharacterIterator c) throws MacroEvaluationException {
        char[] matches = null;
        if(Character.toLowerCase(c.current()) == 't') {
            matches = new char[] { 't', 'r', 'u', 'e' };
        } else if(Character.toLowerCase(c.current()) == 'f') {
            matches = new char[] { 'f', 'a', 'l', 's', 'e' };
        }

        if(matches == null) {
            throw new MacroEvaluationException("Invalid boolean value for argument for macro: " + tokenName);
        }

        for(int i = 0; i < matches.length; ++i) {
            if(c.current() != matches[i]) {
                throw new MacroEvaluationException("Invalid boolean value in macro: " + tokenName);
            }
            c.next();
        }
        argInfoStack.push(new String(matches));
    }

    private String parseNumericalValue(CharacterIterator c) throws MacroEvaluationException {
        StringBuilder builder = new StringBuilder();
        if(c.current() == '-') {
            builder.append(c.current());
            c.next();
        }

        if(c.current() != '0') {
            if(!Character.isDigit(c.current())) {
                throw new MacroEvaluationException("Invalid number value in macro: " + tokenName);
            }

            // we must have a decimal number
            while(Character.isDigit(c.current())) {
                builder.append(c.current());
                c.next();
            }
        } else {
            // we could have a decimal, octal or hex number
            builder.append(c.current());
            c.next();
            if(c.current() == 'x' || c.current() == 'X') {
                // we have a hex number
                while(Character.isDigit (c.current()) || (c.current() >= 'a' && c.current() <= 'f') || (c.current() >= 'A' && c.current() <= 'F')) {
                    builder.append(c.current());
                    c.next();
                }
            } else if(Character.isDigit(c.current()) && c.current() >= '0' && c.current() <= '7') {
                // we have an octal number
                while(Character.isDigit(c.current()) && c.current() >= '0' && c.current() <= '7') {
                    builder.append(c.current());
                    c.next();
                }
            } else if(Character.isDigit(c.current())) {
                // decimal number
                boolean foundDecimal = false;
                while(Character.isDigit(c.current()) || (c.current() == '.' && !foundDecimal)) {
                    if(c.current() == '.') {
                        foundDecimal = true;
                    }
                    builder.append(c.current());
                    c.next();
                }
            }
        }
        return builder.toString();
    }

    boolean addTransform(Transform t) {
        transforms.push(t);
        return true;
    }

    boolean processToken(int currentIndex) throws IOException, InterruptedException, MacroEvaluationException {
        String replacement = null;

        List<TokenMacro> all = new ArrayList<TokenMacro>(TokenMacro.all());
        if(privateTokens!=null) {
            all.addAll(privateTokens.stream().filter(x -> x != null).collect(Collectors.toList()));
        }

        Map<String,String> map = new HashMap<String, String>();
        for (Map.Entry<String, String> e : args.entries()) {
            map.put(e.getKey(),e.getValue());
        }

        for (TokenMacro tm : all) {
            if (tm.acceptsMacroName(tokenName)) {
                try {
                    // first we check if there is a method that takes a run/workspace/etc
                    if(run instanceof AbstractBuild) {
                        AbstractBuild<?,?> build = (AbstractBuild<?, ?>)run;
                        replacement = tm.evaluate(build,listener,tokenName,map,args);
                    } else {
                        replacement = tm.evaluate(run,workspace,listener,tokenName,map,args);
                    }

                    if(tm.hasNestedContent() && recursionLevel < MAX_RECURSION_LEVEL) {
                        replacement = Parser.process(run,workspace,listener,replacement,throwException,privateTokens,recursionLevel+1);
                    }
                } catch(MacroEvaluationException e) {
                    if(throwException) {
                        throw e;
                    } else {
                        replacement = String.format("[Error replacing '%s' - %s]", tokenName, e.getMessage());
                    }
                }
                break;
            }
        }

        if (replacement == null && throwException)
            throw new MacroEvaluationException(String.format("Unrecognized macro '%s' in '%s'", tokenName, stringWithMacro));

        if (replacement == null && !throwException) { // just put the token back in since we don't want to throw the exception
            output.append(stringWithMacro.substring(tokenStartIndex, currentIndex+1));
        } else if (replacement != null) {
            while(transforms != null && transforms.size() > 0) {
                Transform t = transforms.pop();
                replacement = t.transform(replacement);
            }
            output.append(replacement);
        }

        tokenName = "";
        args = null;

        return true;
    }

    boolean startToken(String tokenName) {
        this.tokenName = tokenName;
        if(args == null) {
            args = Multimaps.newListMultimap(new TreeMap<>(), () -> new ArrayList<String>());
        } else {
            args.clear();
        }
        return true;
    }

    boolean addArg() {
        String value = argInfoStack.pop();
        String name = argInfoStack.pop();
        args.put(name, value);
        return true;
    }

    /**
     * Replaces all the printf-style escape sequences in a string
     * with the appropriate characters.
     *
     * @param escapedString the string containing escapes
     * @return the string with all the escape sequences replaced
     */
    public static String unescapeString(String escapedString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < escapedString.length(); ++i) {
            char c = escapedString.charAt(i);
            if (c == '\\') {
                ++i;
                sb.append(unescapeChar(escapedString.charAt(i)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static char unescapeChar(char escapedChar) {
        switch (escapedChar) {
            case 'b':
                return '\b';
            case 't':
                return '\t';
            case 'n':
                return '\n';
            case 'f':
                return '\f';
            case 'r':
                return '\r';
            default:
                return escapedChar;
        }
    }
}
