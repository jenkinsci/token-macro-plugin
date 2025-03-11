package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.tokenmacro.*;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class LogRegExMacroTest {

    @Test
    void testPropertyFromFileExpansion(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("tester");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                listener.getLogger().println("Test Property 123");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();

        assertEquals("value 123", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${LOG_REGEX,regex=\"Test Property (123)\",replacement=\"value \\\\1\"}"));
    }
}
