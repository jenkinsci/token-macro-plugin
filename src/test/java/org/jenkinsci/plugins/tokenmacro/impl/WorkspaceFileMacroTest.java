package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.jvnet.hudson.test.TestBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class WorkspaceFileMacroTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test1() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        TaskListener listener = StreamTaskListener.fromStdout();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                build.getWorkspace().child("foo").write("Hello, world!", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        WorkspaceFileMacro content = new WorkspaceFileMacro();
        content.path = "foo";
        assertEquals("Hello, world!", content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME));
        content.path = "no-such-file";
        assertEquals("ERROR: File 'no-such-file' does not exist", content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME));
        content.fileNotFoundMessage = "No '%s' for you!";
        assertEquals("No 'no-such-file' for you!", content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME));
    }

    @Test
    public void testMaxLines() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        TaskListener listener = StreamTaskListener.fromStdout();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                FilePath file = build.getWorkspace().child("foo");
                try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file.write()))) {
                    for(int i = 0; i < 1000; i++) {
                        writer.write("Hello, world! " + i + "\n");
                    }
                }
                return true;
            }
        });
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        WorkspaceFileMacro content = new WorkspaceFileMacro();
        content.path = "foo";
        content.maxLines = 10;
        String output = content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME);
        String[] lines = output.split("\n");
        assertEquals(10, lines.length);
        for(int i = 0; i < 10; i++) {
            assertEquals("Hello, world! " + i, lines[i]);
        }
    }
}
