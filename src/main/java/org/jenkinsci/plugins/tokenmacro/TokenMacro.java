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
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.apache.tools.ant.taskdefs.Parallel;

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
 * &lt;help xmlons="/lib/token-macro"/&gt;
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

    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap)
            throws MacroEvaluationException, IOException, InterruptedException
    {
        return macroName + " is not supported in this context";
    }

    /**
     * Returns true if this object allows for nested content replacements.
     *
     * @return true
     *      ... to have the replaced text passed again to {@link #expand(AbstractBuild, TaskListener, String)} for additional expansion.
     */
    public boolean hasNestedContent() {
        return false;
    }

    public List<String> getAcceptedMacroNames() {
        return Collections.EMPTY_LIST;
    }

    public static List<String> getAutoCompleteList(String input) {
        List<String> result = new ArrayList<>();
        for(TokenMacro m : all()) {
            for(String name : m.getAcceptedMacroNames()) {
                if(name.isEmpty())
                    continue;

                if(name.startsWith(input)) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    /**
     * All registered extension points.
     * 
     * @return All registered token macro classes.
     */
    public static ExtensionList<TokenMacro> all() {
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            return jenkins.getExtensionList(TokenMacro.class);
        } else {
            return ExtensionList.create((Jenkins) null, TokenMacro.class);
        }
    }

    public static String expand(AbstractBuild<?,?> context, TaskListener listener, String stringWithMacro) throws MacroEvaluationException, IOException, InterruptedException {
        return expand(context, listener, stringWithMacro, true, null);
    }

    public static String expand(AbstractBuild<?,?> context, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens) throws MacroEvaluationException, IOException, InterruptedException {
        return Parser.process(context,listener,stringWithMacro,throwException,privateTokens);
    }

    public static String expand(Run<?, ?> run, FilePath workspace, TaskListener listener, String stringWithMacro) throws MacroEvaluationException, IOException, InterruptedException {
        return expand(run, workspace, listener, stringWithMacro, true, null);
    }

    public static String expand(Run<?, ?> run, FilePath workspace, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens) throws MacroEvaluationException, IOException, InterruptedException {
        return Parser.process(run,workspace,listener,stringWithMacro,throwException,privateTokens);
    }

    public static String expandAll(AbstractBuild<?,?> context, TaskListener listener, String stringWithMacro) throws MacroEvaluationException, IOException, InterruptedException {
        return expandAll(context,listener,stringWithMacro,true,null);
    }

    public static String expandAll(Run<?,?> run, FilePath workspace, TaskListener listener, String stringWithMacro) throws MacroEvaluationException, IOException, InterruptedException {
        return expandAll(run,workspace,listener,stringWithMacro,true,null);
    }

    public static String expandAll(AbstractBuild<?,?> context, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens) throws MacroEvaluationException, IOException, InterruptedException {
        return expandAll(context,getWorkspace(context),listener,stringWithMacro,throwException,privateTokens);
    }

    public static String expandAll(Run<?,?> run, FilePath workspace, TaskListener listener, String stringWithMacro, boolean throwException, List<TokenMacro> privateTokens) throws MacroEvaluationException, IOException, InterruptedException {
        // Do nothing for an empty String
        if (stringWithMacro==null || stringWithMacro.length()==0) return stringWithMacro;

        // Expand environment variables
        stringWithMacro = stringWithMacro.replaceAll("\\$\\$", "\\$\\$\\$\\$");
        String s = run.getEnvironment(listener).expand(stringWithMacro);
        // Expand build variables
        s = s.replaceAll("\\$\\$", "\\$\\$\\$\\$");
        if(run instanceof AbstractBuild) {
            AbstractBuild<?,?> build = (AbstractBuild<?, ?>)run;
            s = Util.replaceMacro(s, build.getBuildVariableResolver());
        }
        // Expand Macros
        s = expand(run,workspace,listener,s,throwException,privateTokens);
        return s;
    }
    
    /**
     * Gets a workspace of the build in the macro.
     * @param context Build
     * @return Retrieved workspace
     * @throws MacroEvaluationException  Workspace is inaccessible
     */
    @Nonnull
    protected static FilePath getWorkspace(@Nonnull AbstractBuild<?, ?> context)
            throws MacroEvaluationException {
        final FilePath workspace = context.getWorkspace();
        if (workspace == null) {
            throw new MacroEvaluationException("Workspace is not accessible");
        }
        return workspace;
    }

    /**
     * Looks for a previous build, so long as that is in fact completed.
     * Necessary since {@link hudson.tasks.Builder#getRequiredMonitorService} does not wait for the
     * previous build, so in the case of parallel-capable jobs, we need to
     * behave sensibly when a later build actually finishes before an earlier
     * one.
     *
     * @param run a run for which we may be sending mail
     * @param listener a listener to which we may print warnings in case the
     * actual previous run is still in progress
     * @return the previous run, or null if that run is missing, or is still in progress
     */
    @CheckForNull
    public static Run<?, ?> getPreviousRun(@Nonnull Run<?, ?> run, TaskListener listener) {
        Run<?, ?> previousRun = run.getPreviousBuild();
        if (previousRun != null && previousRun.isBuilding()) {
            listener.getLogger().println(Messages.TokenMacro_Run_still_in_progress(previousRun.getDisplayName()));
            return null;
        } else {
            return previousRun;
        }
    }
}
