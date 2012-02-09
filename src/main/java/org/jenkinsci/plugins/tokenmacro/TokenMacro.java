/*
 * The MIT License
 *
 * Copyright 2011 CloudBees, Inc.
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

import com.google.common.collect.ListMultimap;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A macro that expands to text values in the context of a {@link AbstractBuild}.
 *
 * <p>
 * Various plugins, such as email-ext and description-setter, has this concept of producing some textual
 * value out of a build (to become the e-mail content/subject, to be come the build description, etc),
 * and the user is allowed to configure how those strings look like.
 *
 * <p>
 * In such situation, it is useful to have a notion of "macro tokens", one that look like like <tt>${foobar}</tt>,
 * and expands to some string value when evaluated. This is exactly such an abstraction, and it is placed
 * in its own plugin in the hope that it's reusable by other plugins.
 *
 * <p>
 * In more general form, the macro would have the following syntax structure:
 *
 * <pre>
 * ${MACRONAME [, ARG, ARG, ...]}
 * ARG := NAME [ = 'value' ]
 * </pre>
 *
 *
 * <h2>Views</h2>
 * <p>
 * Implementation should have <tt>help.jelly</tt> that renders a DT tag that shows the syntax of the macro,
 * followed by a DD tag that shows the details. See existing use of this extension point for the general
 * guide line of the syntax.
 *
 * <p>
 * Plugins interested in using the list of tags can use the "/lib/token-macro" taglib like the following,
 * which expands to the HTML that lists all the tags and their usages:
 *
 * <pre>
 * &lt;help xmlons="/lib/token-macro"/>
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class TokenMacro implements ExtensionPoint {
    /**
     * Returns true if this object can evaluate the macro of the given name.
     *
     * @param macroName
     *      By convention we encourage all caps name.
     *
     * @return true
     *      ... to claim the macro of the given name and have {@link #evaluate(AbstractBuild, TaskListener, String, Map, ListMultimap)} called.
     */
    public abstract boolean acceptsMacroName(String macroName);

    /**
     * Evaluates the macro and produces the token.
     *
     *
     * <h3>Locale</h3>
     * <p>
     * If the token is to produce a human readable text, it should do so by using the implicit locale associated
     * with the calling thread &mdash; see {@code Functions.getCurrentLocale()}.
     *
     * @param context
     *      The build object for which this macro is evaluated.
     * @param listener
     *      If the progress/status needs to be reported to the build console output, this object can be used.
     * @param macroName
     *      The macro name that {@linkplain #acceptsMacroName(String) you accepted}
     * @param arguments
     *      Arguments as a map. If multiple values are specified for one key, this will only retain the last one.
     *      This is passed in separately from {@code argumentMultimap} because
     * @param argumentMultimap
     *      The same arguments, but in a multi-map. If multiple values are specified for one key, all of them
     *      are retained here in the order of appearance. For those macros that support multiple values for the same key
     *      this is more accurate than {@code arguments}, but it's bit more tedious to use.
     *
     * @return
     *      The result of the evaluation. Must not be null.
     *
     * @throws MacroEvaluationException
     *      If the evaluation failed, for example because of the parameter error, and that the error message
     *      should be presented.
     * @throws IOException
     *      Other fatal {@link IOException}s that should leave the stack trace in the console.
     * @throws InterruptedException
     *      If the evaluation involves some remoting operation, user might cancel the build, which results
     *      in an {@link InterruptedException}. Don't catch it, just propagate.
     */
    public abstract String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap)
            throws MacroEvaluationException, IOException, InterruptedException;

    /**
     * Returns true if this object allows for nested content replacements.
     *
     * @return true
     *      ... to have the replaced text passed again to {@link #expand(AbstractBuild, TaskListener, String)} for additional expansion.
     */
    public boolean hasNestedContent() {
        return false;
    }

    /**
     * All registered extension points.
     */
    public static ExtensionList<TokenMacro> all() {
        return Hudson.getInstance().getExtensionList(TokenMacro.class);
    }

    /**
     * Expands all the macros, and throws an exception if there's any problem found.
     *
     * @param stringWithMacro
     *      String that contains macro references in it, like "foo bar ${zot}".
     */
    public static String expand(AbstractBuild<?,?> context, TaskListener listener, String stringWithMacro) throws MacroEvaluationException, IOException, InterruptedException {
        if ( StringUtils.isBlank( stringWithMacro ) ) return stringWithMacro;
        StringBuffer sb = new StringBuffer();
        Tokenizer tokenizer = new Tokenizer(stringWithMacro);

        ExtensionList<TokenMacro> all = all();

        while (tokenizer.find()) {
            String tokenName = tokenizer.getTokenName();
            ListMultimap<String,String> args = tokenizer.getArgs();
            Map<String,String> map = new HashMap<String, String>();
            for (Entry<String, String> e : args.entries()) {
                map.put(e.getKey(),e.getValue());
            }

            String replacement = null;
            for (TokenMacro tm : all) {
                if (tm.acceptsMacroName(tokenName)) {
                    replacement = tm.evaluate(context,listener,tokenName,map,args);
                    if(tm.hasNestedContent()) {
                        replacement = expand(context,listener,replacement);
                    }
                    break;
                }
            }
            if (replacement==null)
                throw new MacroEvaluationException(String.format("Unrecognized macro '%s' in '%s'", tokenName, stringWithMacro));

            tokenizer.appendReplacement(sb, replacement);
        }
        tokenizer.appendTail(sb);

        return sb.toString();
    }

    /**
     * Expands everything that needs to be expanded.
     *
     * Expands all the macros, environment variables, and build variables.
     * Throws an exception if there's any problem found.
     *
     * This should be more convenient than having plugins do all 3 separately.
     *
     * @param stringWithMacro
     *      String that contains macro references in it, like "foo bar ${zot}".
     */
    public static String expandAll(AbstractBuild<?,?> context, TaskListener listener, String stringWithMacro) throws MacroEvaluationException, IOException, InterruptedException {
        // Do nothing for an empty String
        if (stringWithMacro==null || stringWithMacro.length()==0) return stringWithMacro;
        // Expand environment variables
        String s = context.getEnvironment(listener).expand(stringWithMacro);
        // Expand build variables
        s = Util.replaceMacro(s,context.getBuildVariableResolver());
        // Expand Macros
        s = expand(context,listener,s);
        return s;
    }
}
