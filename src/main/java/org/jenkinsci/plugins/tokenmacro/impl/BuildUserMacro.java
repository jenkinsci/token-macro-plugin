package org.jenkinsci.plugins.tokenmacro.impl;

import com.google.common.collect.ListMultimap;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

@Extension
public class BuildUserMacro extends TokenMacro {
    public static final String MACRO_NAME = "BUILD_USER";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(
            AbstractBuild<?, ?> context,
            TaskListener listener,
            String macroName,
            Map<String, String> arguments,
            ListMultimap<String, String> argumentMultimap)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context, null, listener, macroName, arguments, argumentMultimap);
    }

    @Override
    public String evaluate(
            Run<?, ?> run,
            FilePath workspace,
            TaskListener listener,
            String macroName,
            Map<String, String> arguments,
            ListMultimap<String, String> argumentMultimap)
            throws MacroEvaluationException, IOException, InterruptedException {
        Cause.UserIdCause userIdCause = run.getCause(Cause.UserIdCause.class);
        if (userIdCause != null) {
            return userIdCause.getUserId();
        }
        return Messages.TokenMacro_Unknown();
    }
}
