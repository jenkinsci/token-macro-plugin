package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class BuildCauseMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_CAUSE";
    public static final String ALTERNATE_MACRO_NAME = "CAUSE";

    @Parameter
    public String data = null;

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
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, null, listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        List<Cause> causes = new LinkedList<Cause>();
        CauseAction causeAction = run.getAction(CauseAction.class);
        if (causeAction != null) {
            causes = causeAction.getCauses();
        }

        return formatCauses(causes);
    }

    private String formatCauses(List<Cause> causes) {
        if (causes.isEmpty()) {
            return "N/A";
        }

        List<String> causeData = new LinkedList<String>();
        for (Cause cause : causes) {
            if (data != null) {
                if (cause instanceof Cause.UpstreamCause) {
                    Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause) cause;
                    if (data.equals("BUILD_URL")) {
                        causeData.add(upstreamCause.getUpstreamUrl());
                    } else if (data.equals("PROJECT_NAME")) {
                        causeData.add(upstreamCause.getUpstreamProject());
                    } else if (data.equals("BUILD_NUMBER")) {
                        causeData.add("" + upstreamCause.getUpstreamBuild());
                    }
                }
            } else {
                causeData.add(cause.getShortDescription());
            }
        }

        return StringUtils.join(causeData, ", ");
    }
}
