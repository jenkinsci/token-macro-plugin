package org.jenkinsci.plugins.tokenmacro;

import com.google.common.base.Supplier;
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
import org.parboiled.*;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.Var;

import java.io.IOException;
import java.util.*;

/**
 * Created by acearl on 3/6/2016.
 */
public class Parser extends BaseParser<Object> {

    private Stack<Transform> transforms = new Stack<Transform>();
    private StringBuffer output;

    private Run<?, ?> run;
    private FilePath workspace;
    private TaskListener listener;
    private boolean throwException;
    private List<TokenMacro> privateTokens;
    private String stringWithMacro;

    private String tokenName;
    private ListMultimap<String,String> args;

    public Parser(Run<?,?> run, FilePath workspace, TaskListener listener, String stringWithMacro) {
        this.run = run;
        this.workspace = workspace;
        this.listener = listener;
        this.stringWithMacro = stringWithMacro;
        this.output = new StringBuffer();
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    public void setPrivateTokens(List<TokenMacro> privateTokens) {
        this.privateTokens = privateTokens;
    }

    public static String process(AbstractBuild<?,?> build, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens) throws MacroEvaluationException {
        return process(build,build.getWorkspace(),listener,stringWithMacro,throwException,privateTokens);
    }

    public static String process(Run<?, ?> run, FilePath workspace, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens) throws MacroEvaluationException {
        if ( StringUtils.isBlank( stringWithMacro ) ) return stringWithMacro;

        Parser p = Parboiled.createParser(Parser.class, run, workspace, listener, stringWithMacro);
        p.setThrowException(throwException);
        p.setPrivateTokens(privateTokens);

        try {
            new ReportingParseRunner(p.Text()).run(stringWithMacro);
        } catch(Exception e) {
            if(e.getCause() instanceof MacroEvaluationException)
                throw (MacroEvaluationException)e.getCause();
            return String.format("Error processing tokens: %s", e.getMessage());
        }

        return p.output.toString();
    }

    public Rule Text() throws InterruptedException, MacroEvaluationException, IOException {
        return Sequence(
                ZeroOrMore(
                        FirstOf(
                                Token(),
                                Sequence(ANY, appendOutput()))),
                EOI);
    }

    public Rule Token() throws InterruptedException, MacroEvaluationException, IOException {
        return FirstOf(
                EscapedToken(),
                DelimitedToken(),
                NonDelimitedToken());
    }

    Rule DelimitedToken() throws InterruptedException, MacroEvaluationException, IOException {
        return Sequence('$', '{',
                Optional(Sequence('#', addTransform(new ContentLengthTransform()))),
                Sequence(Identifier(), startToken()),
                Optional(Expansion()),
                Optional(Arguments()),
                Optional(Spacing()),
                '}',
                processToken());
    }

    Rule WhiteSpace() {
        return ZeroOrMore(AnyOf(" \t\f"));
    }

    Rule EscapedToken() {
        return FirstOf(EscapedDelimitedToken(), EscapedNonDelimitedToken());
    }

    Rule EscapedDelimitedToken() {
        return Sequence('$', Sequence('$', '{', Identifier(), ZeroOrMore(TestNot('}')), '}'), appendOutput());
    }

    Rule EscapedNonDelimitedToken() {
        return Sequence('$', Sequence('$', Identifier()), appendOutput());
    }

    Rule NonDelimitedToken() throws InterruptedException, MacroEvaluationException, IOException {
        return Sequence('$', Sequence(Identifier(), startToken()), processToken());
    }

    Rule Expansion() {
        return FirstOf(SubstringExpansion(), BeginningMatchExpansion(), EndingMatchExpansion());
    }

    /**
     * Rule for substring expansion, which is of the form ${TOKEN:offset:length}, where length is optional. offset and
     * length can be negative, which then operates from the end of the string.
     */
    Rule SubstringExpansion() {
        final Var<Integer> offset = new Var(0);
        final Var<Integer> length = new Var(Integer.MAX_VALUE);
        final Var<Boolean> isOffsetNegative = new Var(false);
        final Var<Boolean> isLengthNegative = new Var(false);
        return Sequence(
                ':',
                Sequence(Optional(' ', '-', isOffsetNegative.set(true)), IntegerValue(), offset.set(Integer.parseInt(((String)pop()).trim()))),
                Optional(':', Sequence(Optional('-', isLengthNegative.set(true)), IntegerValue(), length.set(Integer.parseInt(((String)pop()).trim())))),
                new Action() {
                    @Override
                    public boolean run(Context context) {
                        return addTransform(new SubstringTransform((isOffsetNegative.get() ? -1 : 1) * offset.get(), (isLengthNegative.get() ? -1 : 1) * length.get()));
                    }
                }
        );
    }

    /**
     * Rule for beginning match expansion, which is of the form${TOKEN#pattern}, where pattern is a regular expression.
     * Will check for match at the beginning of the string, and if matched remove the matching text.
     */
    Rule BeginningMatchExpansion() {
        return Sequence('#',
                ZeroOrMore(
                    FirstOf(
                            NoneOf("\\\"\r\n},"),
                            Sequence('\\', FirstOf(Sequence(Optional('\r'), '\n'), ANY))
                    )
                ),
                addTransform(new BeginningOrEndMatchTransorm(match(), true)));
    }


    Rule EndingMatchExpansion() {
        return Sequence('%',
                ZeroOrMore(
                        FirstOf(
                                NoneOf("\\\"\r\n},"),
                                Sequence('\\', FirstOf(Sequence(Optional('\r'), '\n'), ANY))
                        )
                ),
                addTransform(new BeginningOrEndMatchTransorm(match(), false)));
    }

    Rule Arguments() {
        return ZeroOrMore(Sequence(Spacing(), ',', Spacing(), Sequence(Identifier(), push(match())), Spacing(), '=', Spacing(), ArgumentValue()), addArg());
    }

    Rule Spacing() {
        return ZeroOrMore(AnyOf(" \t"));
    }

    Rule ArgumentValue() {
        return FirstOf(FloatValue(), IntegerValue(), StringValue(), BooleanValue());
    }

    Rule IntegerValue() {
        return Sequence(FirstOf(HexNumeral(), OctalNumeral(), DecimalNumeral()), push(match()));
    }

    Rule HexNumeral() {
        return Sequence('0', IgnoreCase('x'), OneOrMore(HexDigit()));
    }

    Rule OctalNumeral() {
        return Sequence('0', OneOrMore(CharRange('0', '7')));
    }

    Rule DecimalNumeral() {
        return FirstOf('0', Sequence(CharRange('1', '9'), ZeroOrMore(Digit())));
    }

    Rule DecimalFloat() {
        return FirstOf(
                Sequence(OneOrMore(Digit()), '.', ZeroOrMore(Digit()), Optional(Exponent())),
                Sequence('.', OneOrMore(Digit()), Optional(Exponent())),
                Sequence(OneOrMore(Digit()), Exponent()),
                Sequence(OneOrMore(Digit()), Optional(Exponent()))
        );
    }

    Rule Exponent() {
        return Sequence(AnyOf("eE"), Optional(AnyOf("+-")), OneOrMore(Digit()));
    }

    Rule Digit() {
        return CharRange('0', '9');
    }

    @SuppressSubnodes
    Rule HexFloat() {
        return Sequence(HexSignificant(), BinaryExponent());
    }

    Rule HexSignificant() {
        return FirstOf(
                Sequence(FirstOf("0x", "0X"), ZeroOrMore(HexDigit()), '.', OneOrMore(HexDigit())),
                Sequence(HexNumeral(), Optional('.'))
        );
    }

    Rule BinaryExponent() {
        return Sequence(AnyOf("pP"), Optional(AnyOf("+-")), OneOrMore(Digit()));
    }


    Rule StringValue() {
        return Sequence(
                '"',
                ZeroOrMore(
                        FirstOf(
                            NoneOf("\\\"\r\n"),
                            Sequence('\\', FirstOf(Sequence(Optional('\r'), '\n'), ANY))
                        )
                ),
                push(unescapeString(match())),
                '"'
        );
    }

    Rule HexDigit() {
        return FirstOf(CharRange('a', 'f'), CharRange('A', 'F'), CharRange('0', '9'));
    }

    Rule BooleanValue() {
        return Sequence(FirstOf(String("true"), String("false")), push(match()));
    }

    Rule FloatValue() {
        return Sequence(FirstOf(HexFloat(), DecimalFloat()), push(match()));
    }

    Rule Identifier() {
        return Sequence(Letter(), ZeroOrMore(LetterOrDigit()));
    }

    Rule Letter() {
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), '_');
    }

    Rule LetterOrDigit() {
        return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), CharRange('0', '9'), '_');
    }

    boolean addTransform(Transform t) {
        transforms.push(t);
        return true;
    }

    boolean processToken() throws IOException, InterruptedException, MacroEvaluationException {
        String replacement = null;

        List<TokenMacro> all = new ArrayList<TokenMacro>(TokenMacro.all());
        if(privateTokens!=null) {
            all.addAll( privateTokens );
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

                    if(tm.hasNestedContent()) {
                        replacement = Parser.process(run,workspace,listener,replacement,throwException,privateTokens);
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
            output.append(getContext().getInputBuffer().extract(getContext().getStartIndex(), getContext().getCurrentIndex()));
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

    boolean appendOutput() {
        output.append(match());
        return true;
    }

    boolean startToken() {
        tokenName = match();
        if(args == null) {
            args = Multimaps.newListMultimap(new TreeMap<String, Collection<String>>(), new Supplier<List<String>>() {
                public List<String> get() {
                    return new ArrayList<String>();
                }
            });
        } else {
            args.clear();
        }
        return true;
    }

    boolean addArg() {
        String value = (String)pop();
        String name = (String)pop();
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
