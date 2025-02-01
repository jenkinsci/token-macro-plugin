package org.jenkinsci.plugins.tokenmacro.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

@Extension
public class ChangesSinceLastUnstableBuildMacro extends AbstractChangesSinceMacro {

    public static final String MACRO_NAME = "CHANGES_SINCE_LAST_UNSTABLE";
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
        return "Expands to the changes since the last unstable or successful build. ";
    }

    @Override
    @SuppressFBWarnings
    public Run<?, ?> getFirstIncludedRun(Run<?, ?> run, TaskListener listener) {
        Run<?, ?> firstIncludedBuild = run;

        Run<?, ?> prev = TokenMacro.getPreviousRun(firstIncludedBuild, listener);
        while (prev != null && prev.getResult() != null && prev.getResult().isWorseThan(Result.UNSTABLE)) {
            firstIncludedBuild = prev;
            prev = TokenMacro.getPreviousRun(firstIncludedBuild, listener);
        }

        return firstIncludedBuild;
    }
}
