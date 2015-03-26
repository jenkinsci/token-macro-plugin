/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.tokenmacro.impl;

import com.google.common.collect.ListMultimap;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Map;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

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
    public String evaluate(Run<?, ?> context, FilePath workspace, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) 
            throws MacroEvaluationException, IOException, InterruptedException{
        return JenkinsLocationConfiguration.get().getAdminAddress();
    }
    
}
