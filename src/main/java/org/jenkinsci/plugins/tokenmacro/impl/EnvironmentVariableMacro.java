package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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

    @Parameter(alias="default")
    public String def = "";

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context,null,listener,macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        Map<String, String> env = run.getEnvironment(listener);
        if(env.containsKey(var)){
            return env.get(var);
        } else if(StringUtils.isNotBlank(def)) {
            return def;
        }
        return "";
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return MACRO_NAME.equals(macroName);
    }

}
