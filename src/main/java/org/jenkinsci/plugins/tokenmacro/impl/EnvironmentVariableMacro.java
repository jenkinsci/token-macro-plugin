package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    /*
     * Uses reflection to get the EnvVars from the WorkflowRun execution object
     *
     * @param run - the run object
     * @returns The environment variable value or empty string on error
     */
    private String getEnvVarFromWorkflowRun(Run<?,?> run) {
        Class<?> workflowRunClass = run.getClass();
        try {
            if(workflowRunClass.getName().contains("WorkflowRun")) {
                // get the FlowExecution object for this run
                Method getExecution = workflowRunClass.getMethod("getExecution");
                Object execution = getExecution.invoke(run);

                // get a list of executions (as a future)
                Method current = execution.getClass().getMethod("getCurrentExecutions", boolean.class);
                Object currentExecutionsFuture = current.invoke(execution, true);
                // get the actual executions list
                Method get = currentExecutionsFuture.getClass().getMethod("get");
                Object actualExecutions = get.invoke(currentExecutionsFuture);

                // retrieve the first execution, this should be the TokenMacroStep$Execution instance
                Method getItem = List.class.getMethod("get", int.class);
                Object tokenMacroStepExecution = getItem.invoke(actualExecutions, 0);
                Method getContext = tokenMacroStepExecution.getClass().getMethod("getContext");
                Object context = getContext.invoke(tokenMacroStepExecution);

                // now we can get the EnvVars context item
                Method contextGet = context.getClass().getMethod("get", Class.class);
                Map<String, String> envVars = (Map<String,String>)contextGet.invoke(context, EnvVars.class);
                if(envVars != null && envVars.containsKey(var)) {
                    return envVars.get(var);
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            // don't do anything here
        }
        return "";
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        String res = getEnvVarFromWorkflowRun(run);
        if(StringUtils.isBlank(res)) {
            Map<String, String> env = run.getEnvironment(listener);
            if (env.containsKey(var)) {
                return env.get(var);
            }
        }

        if(StringUtils.isBlank(res) && StringUtils.isNotBlank(def)) {
            res = def;
        }

        return res;
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return MACRO_NAME.equals(macroName);
    }

}
