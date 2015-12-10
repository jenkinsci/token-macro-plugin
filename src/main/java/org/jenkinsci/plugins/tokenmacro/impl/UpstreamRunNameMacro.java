package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.*;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import javax.annotation.Nonnull;
import java.io.IOException;

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
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        CauseAction action = context.getAction(CauseAction.class);
        if(action != null) {
            for(Cause c : action.getCauses()) {
                if(c instanceof Cause.UpstreamCause) {
                    final Cause.UpstreamCause u = (Cause.UpstreamCause)c;
                    if(u.getUpstreamRun() != null) {
                        return u.getUpstreamRun().getDisplayName();
                    }
                }
            }
        }

        return "Unknown";
    }
}
