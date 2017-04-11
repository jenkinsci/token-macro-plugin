package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Launcher;
import hudson.model.*;
import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Kohsuke Kawaguchi
 */
public class JsonFileMacroTest {
    private TaskListener listener;
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testJsonFromFileExpansion() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' } }","UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("1.2.3",TokenMacro.expand(b, StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\",path=\"project.version\"}"));
    }

    @Test
    public void testJsonKeyMissing() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' } }","UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("Error: The key 'the' does not exist in the JSON",TokenMacro.expand(b, StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\",path=\"project.the.version\"}"));
    }

    @Test
    public void testJsonPathExhausted() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' } }","UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("Error: Found primitive type at key 'version' before exhausting path",TokenMacro.expand(b, StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\",path=\"project.version.another\"}"));
    }

    @Test
    public void testJsonExpr() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' } }","UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("1.2.3",TokenMacro.expand(b, StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\",expr=\"$.project['version']\"}"));
    }

    @Test
    public void testExprOverrides() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' }, 'foo' : 'bar' }","UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("bar",TokenMacro.expand(b, StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\",path=\"project.version\",expr=\"$.foo\"}"));
    }

    @Test
    public void testNoPathOrExpr() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("test.json").write("{'project' : { 'version' : '1.2.3' }, 'foo' : 'bar' }","UTF-8");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        assertEquals("You must specify the path or expr parameter",TokenMacro.expand(b, StreamTaskListener.fromStdout(),
                "${JSON,file=\"test.json\"}"));
    }
}
