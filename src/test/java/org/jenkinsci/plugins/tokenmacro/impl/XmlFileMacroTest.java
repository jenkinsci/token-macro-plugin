package org.jenkinsci.plugins.tokenmacro.impl;

import static junit.framework.TestCase.assertEquals;

import hudson.Launcher;
import hudson.model.*;
import hudson.util.StreamTaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
 * @author Kohsuke Kawaguchi
 */
public class XmlFileMacroTest {
    private TaskListener listener;

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testXmlFromFileExpansion() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.xml").write("<project><version>1.2.3</version></project>", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals(
                "1.2.3",
                TokenMacro.expand(
                        b,
                        StreamTaskListener.fromStdout(),
                        "${XML,file=\"test.xml\",xpath=\"/project/version/text()\"}"));
    }
}
