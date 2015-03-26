package org.jenkinsci.plugins.tokenmacro.impl;

import com.google.common.collect.ListMultimap;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Map;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Token that expands variables from the build environment.
 */
@Extension
public class EnvironmentVariableMacro extends DataBoundTokenMacro {

    @Parameter(required=true)
    public String var = "";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("ENV");
    }

    @Override
    public String evaluate(Run<?, ?> context, FilePath workspace, TaskListener listener, String macroName) 
            throws MacroEvaluationException, IOException, InterruptedException {
        Map<String, String> env = context.getEnvironment(listener);
        if(env.containsKey(var)){
            return env.get(var);
        }
        return "";
    }
}
