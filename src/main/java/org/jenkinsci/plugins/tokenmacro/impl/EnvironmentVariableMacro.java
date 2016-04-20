package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Token that expands variables from the build environment.
 */
@Extension
public class EnvironmentVariableMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "ENV";

    @Parameter(required=true)
    public String var = "";

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        Map<String, String> env = context.getEnvironment(listener);
        if(env.containsKey(var)){
            return env.get(var);
        }
        return "";
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return MACRO_NAME.equals(macroName);
    }

}
