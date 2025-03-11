package org.jenkinsci.plugins.tokenmacro;

import hudson.Launcher;
import hudson.model.*;
import hudson.util.StreamTaskListener;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by acearl on 2/24/2016.
 */
@WithJenkins
class TransformTest {

    @Test
    void testContentLengthTransform(JenkinsRule j) throws Exception {
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
        assertEquals("7", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${#PROPFILE,file=\"test.properties\",property=\"test.property\"}"));
    }

    @Test
    void testSubstringExpansionTransform(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.properties").write("test.property=01234567890abcdefgh", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();

        // tests taken from https://www.gnu.org/software/bash/manual/html_node/Shell-Parameter-Expansion.html
        assertEquals("7890abcdefgh", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE:7,file=\"test.properties\",property=\"test.property\"}"));

        assertEquals("", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE:7:0,file=\"test.properties\",property=\"test.property\"}"));

        assertEquals("78", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE:7:2,file=\"test.properties\",property=\"test.property\"}"));

        assertEquals("7890abcdef", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE:7:-2,file=\"test.properties\",property=\"test.property\"}"));

        assertEquals("bcdefgh", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE: -7,file=\"test.properties\",property=\"test.property\"}"));

        assertEquals("", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE: -7:0,file=\"test.properties\",property=\"test.property\"}"));

        assertEquals("bc", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE: -7:2,file=\"test.properties\",property=\"test.property\"}"));

        assertEquals("bcdef", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${PROPFILE: -7:-2,file=\"test.properties\",property=\"test.property\"}"));
    }

    @Test
    void testBeginningOrEndingMatch(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        FreeStyleBuild b = project.scheduleBuild2(0).get();

        assertEquals("master", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${DUMMY#origin/, arg=\"origin/master\"}"));

        assertEquals("origin", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${DUMMY%/master, arg=\"origin/master\"}"));

        assertEquals("origin", TokenMacro.expand(
                b, StreamTaskListener.fromStdout(), "${DUMMY%/master\\,foo, arg=\"origin/master,foo\"}"));
    }

    @TestExtension
    public static class DummyMacro extends DataBoundTokenMacro {
        private static final String MACRO_NAME = "DUMMY";

        @Parameter
        public String arg;

        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals(MACRO_NAME);
        }

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) {
            return arg;
        }
    }
}
