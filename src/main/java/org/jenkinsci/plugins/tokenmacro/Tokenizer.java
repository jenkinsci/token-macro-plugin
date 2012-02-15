/*
 * The MIT License
 *
 * Copyright 2009- Kyle Sweeney, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.tokenmacro;

import com.google.common.base.Supplier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses strings with macro references.
 *
 * @author kyle.sweeney@valtech.com
 * @author Kohsuke Kawaguchi
 */
class Tokenizer {

    private static final String tokenNameRegex = "[a-zA-Z0-9_]+";

    private static final String numberRegex = "-?[0-9]+(\\.[0-9]*)?";

    private static final String boolRegex = "(true)|(false)";
    // Sequence of (1) not \ " CR LF and (2) \ followed by non line terminator

    private static final String stringRegex = "\"([^\\\\\"\\r\\n]|(\\\\.))*\"";

    private static final String valueRegex = "(" + numberRegex + ")|(" + boolRegex + ")|(" + stringRegex + ")";

    private static final String spaceRegex = "[ \\t]*";

    private static final String argRegex = "(" + tokenNameRegex + ")" + spaceRegex + "=" + spaceRegex + "(" + valueRegex + ")";

    private static final String argsRegex = "((" + spaceRegex + "," + spaceRegex + argRegex + ")*)";

    private static final String delimitedTokenRegex = "\\{" + spaceRegex + "(" + tokenNameRegex + ")" + argsRegex + spaceRegex + "\\}";

    private static final String tokenRegex = "(?<!\\\\)\\$((" + tokenNameRegex + ")|(" + delimitedTokenRegex + "))";

    private static final Pattern argPattern = Pattern.compile(argRegex);

    private static final Pattern tokenPattern = Pattern.compile(tokenRegex);

    private final Matcher tokenMatcher;

    private String tokenName = null;

    private ListMultimap<String,String> args = null;

    Tokenizer(String origText) {
        tokenMatcher = tokenPattern.matcher(origText);
    }

    String getTokenName() {
        return tokenName;
    }

    ListMultimap<String,String> getArgs() {
        return args;
    }

    String group() {
        return tokenMatcher.group();
    }

    boolean find() {
        if (tokenMatcher.find()) {
            tokenName = tokenMatcher.group(2);
            if (tokenName == null) {
                tokenName = tokenMatcher.group(4);
            }
            args = Multimaps.newListMultimap(new TreeMap<String, Collection<String>>(),new Supplier<List<String>>() {
                public List<String> get() {
                    return new ArrayList<String>();
                }
            });
            if (tokenMatcher.group(5) != null) {
                parseArgs(tokenMatcher.group(5), args);
            }
            return true;
        } else {
            return false;
        }
    }

    static void parseArgs(String argsString, ListMultimap<String,String> args) {
        Matcher argMatcher = argPattern.matcher(argsString);
        while (argMatcher.find()) {
            String arg;
            if (argMatcher.group(3) != null) {
                // number
                arg = argMatcher.group(3);
//                if (argMatcher.group(4) != null) {
//                    arg = Float.valueOf();
//                } else {
//                    arg = Integer.valueOf(argMatcher.group(3));
//                }
            } else if (argMatcher.group(5) != null) {
                // boolean
                if (argMatcher.group(6) != null) {
//                    arg = Boolean.TRUE;
                    arg = "true";
                } else {
//                    arg = Boolean.FALSE;
                    arg = "false";
                }
            } else { // if (argMatcher.group(8) != null) {
                // string
                arg = unescapeString(argMatcher.group(8));
            }
            args.put(argMatcher.group(1), arg);
        }
    }

    void appendReplacement(StringBuffer sb, String replacement) {
        tokenMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }

    void appendTail(StringBuffer sb) {
        tokenMatcher.appendTail(sb);
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
        for (int i = 1; i < escapedString.length() - 1; ++i) {
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
