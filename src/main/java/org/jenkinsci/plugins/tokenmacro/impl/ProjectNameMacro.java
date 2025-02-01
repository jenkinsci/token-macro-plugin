package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@Extension
public class ProjectNameMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "PROJECT_NAME";
    public static final String MACRO_NAME2 = "PROJECT_DISPLAY_NAME";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME) || macroName.equals(MACRO_NAME2);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        List<String> macroNames = new ArrayList<>();
        Collections.addAll(macroNames, MACRO_NAME, MACRO_NAME2);
        return macroNames;
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, null, listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> build, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        if (macroName.equals(MACRO_NAME2)) {
            return build.getParent().getDisplayName();
        }
        return build.getParent().getFullDisplayName();
    }
}
