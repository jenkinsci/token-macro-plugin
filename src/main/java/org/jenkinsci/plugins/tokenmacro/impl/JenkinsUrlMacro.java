package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class JenkinsUrlMacro extends DataBoundTokenMacro {

    private static final String MACRO_NAME = "JENKINS_URL";
    private static final String ALTERNATE_MACRO_NAME = "HUDSON_URL";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME) || macroName.equals(ALTERNATE_MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        List<String> macroNames = new ArrayList<>();
        Collections.addAll(macroNames, MACRO_NAME, ALTERNATE_MACRO_NAME);
        return macroNames;
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context, null, listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        String jenkinsUrl = Jenkins.getActiveInstance().getRootUrl();

        if (jenkinsUrl == null) {
            return "";
        }

        if (!jenkinsUrl.endsWith("/")) {
            jenkinsUrl += "/";
        }

        return Util.encode(jenkinsUrl);
    }
}
