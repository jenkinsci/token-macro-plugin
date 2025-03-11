package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Launcher;
import hudson.model.*;
import hudson.util.StreamTaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class JsonFileMacroTest {

    @Test
    void testJsonFromFileExpansion(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' } }", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("1.2.3", TokenMacro.expand(
                b, StreamTaskListener.fromStdout(), "${JSON,file=\"test.json\",path=\"project.version\"}"));
    }

    @Test
    void testJsonKeyMissing(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' } }", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("Error: The key 'the' does not exist in the JSON", TokenMacro.expand(
                b, StreamTaskListener.fromStdout(), "${JSON,file=\"test.json\",path=\"project.the.version\"}"));
    }

    @Test
    void testJsonPathExhausted(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' } }", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("Error: Found primitive type at key 'version' before exhausting path", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\",path=\"project.version.another\"}"));
    }

    @Test
    void testJsonExpr(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' } }", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("1.2.3", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\",expr=\"$.project['version']\"}"));
    }

    @Test
    void testExprOverrides(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("test.json")
                        .write("{'project' : { 'version' : '1.2.3' }, 'foo' : 'bar' }", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("bar", TokenMacro.expand(
                b,
                StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\",path=\"project.version\",expr=\"$.foo\"}"));
    }

    @Test
    void testNoPathOrExpr(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("test.json")
                        .write("{'project' : { 'version' : '1.2.3' }, 'foo' : 'bar' }", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("You must specify the path or expr parameter", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${JSON,file=\"test.json\"}"));
    }
}
