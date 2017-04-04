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
        assertEquals("foo",TokenMacro.expand(b, StreamTaskListener.fromStdout(),"${ENV,var=\"JOB_NAME\"}"));
    }

    @Test
    public void testEnvironmentVariableExpansionDefault() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        assertEquals("You got the default",TokenMacro.expand(b, StreamTaskListener.fromStdout(),"${ENV,var=\"JORB_NAME\", default=\"You got the default\"}"));
    }

    @Test
    public void testEnvironmentVariableExpansionMissingNoDefault() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        assertEquals("",TokenMacro.expand(b, StreamTaskListener.fromStdout(),"${ENV,var=\"JORB_NAME\"}"));
    }
}
