package org.jenkinsci.plugins.tokenmacro.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

/**
 * Token that expands variables from the build environment.
 */
@Extension
public class EnvironmentVariableMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "ENV";

    @Parameter(required = true)
    public String var = "";

    @Parameter(alias = "default")
    public String def = "";

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context, null, listener, macroName);
    }

    /*
     * Uses reflection to get the EnvVars from the WorkflowRun execution object
     *
     * @param run - the run object
     * @returns The environment variable value or empty string on error
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
    private String getEnvVarFromWorkflowRun(Run<?, ?> run) {
        try {
            WorkflowRun workflowRun = (WorkflowRun) run;

            FlowExecution execution = workflowRun.getExecution();
            if (execution != null) {
                List<StepExecution> actualExecutions =
                        execution.getCurrentExecutions(true).get();

                StepContext context = actualExecutions.get(0).getContext();
                Map<String, String> vars = context.get(EnvVars.class);
                if (vars != null && vars.containsKey(var)) {
                    return vars.get(var);
                }
            }
        } catch (Exception e) {
            // don't do anything here
        }
        return "";
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        String res = "";
        if (Jenkins.get().getPlugin("workflow-job") != null) {
            res = getEnvVarFromWorkflowRun(run);
        }

        if (StringUtils.isBlank(res)) {
            Map<String, String> env = run.getEnvironment(listener);
            if (env.containsKey(var)) {
                return env.get(var);
            }
        }

        if (StringUtils.isBlank(res) && StringUtils.isNotBlank(def)) {
            res = def;
        }

        return res;
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return MACRO_NAME.equals(macroName);
    }
}
