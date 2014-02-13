package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.IOException;
import static junit.framework.TestCase.assertEquals;
import org.jenkinsci.plugins.tokenmacro.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
 * @author Kohsuke Kawaguchi
 */
public class PropertyFromFileMacroTest {
    private TaskListener listener;
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testPropertyFromFileExpansion() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
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
