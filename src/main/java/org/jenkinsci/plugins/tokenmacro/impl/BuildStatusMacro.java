package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

@Extension
public class BuildStatusMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_STATUS";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build,null,listener,macroName);
    }

    @Override
    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        // In the case of pipeline jobs, if the status hasn't been set to a non-null value, then it is considered "success"
        if (!(run instanceof AbstractBuild) && null == run.getResult()) {
            // TODO this makes little sense. Why not just use Building as below?
            return Result.SUCCESS.toString();
        }

        // Build can be "building" when the pre-build trigger is used. (and in this case there is not result set yet for the build)
        // Reporting "success", "still failing", etc doesn't make sense in this case.
        // When using on matrix build, the build is still in building stage when matrix aggregator end build trigger is fired, though
        if ( (run.isBuilding()) && (null == run.getResult())) {
            return "Building";
        }

        Result buildResult = run.getResult();
        if (buildResult == Result.FAILURE) {
            Run<?,?> prevBuild = TokenMacro.getPreviousRun(run, listener);
            if (prevBuild != null && (prevBuild.getResult() == Result.FAILURE)) {
                return "Still Failing";
            } else {
                return "Failure";
            }
        } else if (buildResult == Result.UNSTABLE) {
            Run<?,?> prevRun = TokenMacro.getPreviousRun(run, listener);
            while (prevRun != null) {
                //iterate through previous builds
                //(fail_or_aborted)* and then an unstable : return still unstable
                //(fail_or_aborted)* and then successful : return unstable
                if (prevRun.getResult() == Result.UNSTABLE) {
                    return "Still Unstable";
                } else if (prevRun.getResult() == Result.SUCCESS) {
                    return "Unstable";
                }
                prevRun = TokenMacro.getPreviousRun(prevRun, listener);
            }
            return "Unstable";
        } else if (buildResult == Result.SUCCESS) {
            Run<?,?> prevBuild = TokenMacro.getPreviousRun(run, listener);
            if (prevBuild != null && (prevBuild.getResult() == Result.UNSTABLE || prevBuild.getResult() == Result.FAILURE)) {
                return "Fixed";
            } else {
                return "Successful";
            }
        } else if (buildResult == Result.NOT_BUILT) {
            return "Not Built";
        } else if (buildResult == Result.ABORTED) {
            return "Aborted";
        }

        return "Unknown";
    }
}
