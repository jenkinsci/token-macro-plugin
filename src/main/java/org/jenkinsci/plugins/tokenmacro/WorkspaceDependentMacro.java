package org.jenkinsci.plugins.tokenmacro;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import java.io.IOException;

public abstract class WorkspaceDependentMacro extends DataBoundTokenMacro {

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context, context.getWorkspace(), listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, @CheckForNull FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        if (workspace == null) {
            throw new MacroEvaluationException("Macro '" + macroName + "' can ony be evaluated in a workspace.");
        }

        String root = workspace.getRemote();
        return workspace.act(getCallable(run, root, listener));
    }

    public abstract Callable<String, IOException> getCallable(Run<?, ?> run, String root, TaskListener listener);

    @Override
    public boolean acceptsMacroName(String macroName) {
        return getAcceptedMacroNames().contains(macroName);
    }
}
