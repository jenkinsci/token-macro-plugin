/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.tokenmacro.impl;

import com.google.common.collect.ListMultimap;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 *
 * @author acearl
 */
@Extension
public class AdminEmailMacro extends TokenMacro {
    
    public static final String MACRO_NAME = "ADMIN_EMAIL";

    @Override
    public boolean acceptsMacroName(String macroName) {
        return MACRO_NAME.equals(macroName);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context,null,listener,macroName,arguments,argumentMultimap);
    }

    @Override
    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
        return getAdminAddress();
    }

    @NonNull
    public String getAdminAddress() throws MacroEvaluationException {
        JenkinsLocationConfiguration configuration = JenkinsLocationConfiguration.get();
        if (configuration == null) {
            throw new MacroEvaluationException("Jenkins location configuration is not accessible");
        }
        return configuration.getAdminAddress();
    }
}
