package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Created by acearl on 10/23/2015.
 */
@WithJenkins
class UpstreamRunNameMacroTest {

    @Test
    @Issue("JENKINS-27542")
    void testNoUpstreamJob(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        TaskListener listener = StreamTaskListener.fromStdout();
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        UpstreamRunNameMacro content = new UpstreamRunNameMacro();
        assertEquals("Unknown", content.evaluate(build, listener, UpstreamRunNameMacro.MACRO_NAME));
    }

    @Test
    @Issue("JENKINS-27542")
    void testUpstreamJob(JenkinsRule j) throws Exception {
        FreeStyleProject upstream = j.createFreeStyleProject("FOO");
        TaskListener listener = StreamTaskListener.fromStdout();

        FreeStyleProject project = j.createFreeStyleProject("BAR");

        FreeStyleBuild upstreamBuild = upstream.scheduleBuild2(0).get();
        upstreamBuild.setDisplayName("FOO");
        Cause.UpstreamCause cause = new Cause.UpstreamCause(upstreamBuild);

        FreeStyleBuild build = project.scheduleBuild2(0, cause).get();

        UpstreamRunNameMacro content = new UpstreamRunNameMacro();
        assertEquals("FOO", content.evaluate(build, listener, UpstreamRunNameMacro.MACRO_NAME));
    }
}
