package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.tokenmacro.*;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class EnvironmentVariableMacroTest {

    @Test
    void testEnvironmentVariableExpansion(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        assertEquals("foo", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${ENV,var=\"JOB_NAME\"}"));
    }

    @Test
    void testEnvironmentVariableExpansionDefault(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        assertEquals("You got the default", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${ENV,var=\"JORB_NAME\", default=\"You got the default\"}"));
    }

    @Test
    void testEnvironmentVariableExpansionMissingNoDefault(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        assertEquals("", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${ENV,var=\"JORB_NAME\"}"));
    }
}
