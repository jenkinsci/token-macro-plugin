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

/**
 * @author Kohsuke Kawaguchi
 */
public class PropertyFromFileMacroTest extends HudsonTestCase {
    private TaskListener listener;

    public void testPropertyFromFileExpansion() throws Exception {
        FreeStyleProject project = createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.properties").write("test.property=success","UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();

        listener = new StreamTaskListener(System.out);
        assertEquals("success",TokenMacro.expand(b, listener, "${PROPFILE,file=\"test.properties\",property=\"test.property\"}"));
    }
}
