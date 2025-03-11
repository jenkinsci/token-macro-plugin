package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.StreamTaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.*;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class PropertyFromFileMacroTest {

    @Test
    void testPropertyFromFileExpansion(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.properties").write("test.property=success", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("success", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE,file=\"test.properties\",property=\"test.property\"}"));
    }
}
