package org.jenkinsci.plugins.tokenmacro.impl;

import org.jenkinsci.plugins.tokenmacro.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.StreamTaskListener;
import static junit.framework.TestCase.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author Kohsuke Kawaguchi
 */
public class EnvironmentVariableMacroTest {
    private StreamTaskListener listener;
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testEnvironmentVariableExpansion() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        
        listener = StreamTaskListener.fromStdout();
        assertEquals("foo",TokenMacro.expand(b, listener,"${ENV,var=\"JOB_NAME\"}"));
    }
}
