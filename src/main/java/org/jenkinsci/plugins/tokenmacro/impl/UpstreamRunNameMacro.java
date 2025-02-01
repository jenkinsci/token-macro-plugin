package org.jenkinsci.plugins.tokenmacro.impl;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.model.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Created by acearl on 10/22/2015.
 */
public class UpstreamRunNameMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "UPSTREAM_RUN_NAME";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return MACRO_NAME.equals(macroName);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context, null, listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        CauseAction action = run.getAction(CauseAction.class);
        if (action != null) {
            for (Cause c : action.getCauses()) {
                if (c instanceof Cause.UpstreamCause) {
                    Run<?, ?> upstream = getUpstreamRun((Cause.UpstreamCause) c);
                    return upstream.getDisplayName();
                }
            }
        }
        return "Unknown";
    }

    @NonNull
    @SuppressFBWarnings
    private Run<?, ?> getUpstreamRun(Cause.UpstreamCause cause) throws MacroEvaluationException {
        if (cause.getUpstreamRun() == null) throw new MacroEvaluationException("Upstream run is not available");
        return cause.getUpstreamRun();
    }
}
