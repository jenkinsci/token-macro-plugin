package org.jenkinsci.plugins.tokenmacro.impl;

import com.google.common.collect.ListMultimap;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.io.IOException;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class BuildUrlMacro extends TokenMacro {
    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("BUILD_URL");
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
        return Hudson.getInstance().getRootUrl() + context.getUrl();
    }
}
