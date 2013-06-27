package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.*;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

public class LogRegExMacroTest extends HudsonTestCase {
    private TaskListener listener;

    public void testPropertyFromFileExpansion() throws Exception {
        FreeStyleProject project = createFreeStyleProject("tester");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                listener.getLogger().println("Test Property 123");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();

        listener = new StreamTaskListener(System.out);
        assertEquals("value 123",TokenMacro.expand(b, listener, "${LOG_REGEX,regex=\"Test Property (123)\",replacement=\"value \\\\1\"}"));
    }
}
