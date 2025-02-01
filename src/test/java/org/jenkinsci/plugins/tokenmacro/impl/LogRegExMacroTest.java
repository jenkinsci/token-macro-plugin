package org.jenkinsci.plugins.tokenmacro.impl;

import static junit.framework.TestCase.assertEquals;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.StreamTaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

public class LogRegExMacroTest {
    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testPropertyFromFileExpansion() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("tester");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                listener.getLogger().println("Test Property 123");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();

        assertEquals(
                "value 123",
                TokenMacro.expand(
                        b,
                        StreamTaskListener.fromStdout(),
                        "${LOG_REGEX,regex=\"Test Property (123)\",replacement=\"value \\\\1\"}"));
    }
}
