package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.util.Collections;
import java.util.List;

@Extension
public class ChangesSinceLastSuccessfulBuildMacro
        extends AbstractChangesSinceMacro {

    public static final String MACRO_NAME = "CHANGES_SINCE_LAST_SUCCESS";
    private static final String FORMAT_DEFAULT_VALUE = "Changes for Build #%n\\n%c\\n";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String getDefaultFormatValue() {
        return FORMAT_DEFAULT_VALUE;
    }

    @Override
    public String getShortHelpDescription() {
        return "Displays the changes since the last successful build.";
    }

    @Override
    public Run<?,?> getFirstIncludedRun(Run<?,?> build, TaskListener listener) {
        Run<?,?> firstIncludedBuild = build;

        Run<?,?> prev = TokenMacro.getPreviousRun(firstIncludedBuild, listener);
        while (prev != null && prev.getResult() != Result.SUCCESS) {
            firstIncludedBuild = prev;
            prev = TokenMacro.getPreviousRun(firstIncludedBuild, listener);
        }

        return firstIncludedBuild;
    }
}

