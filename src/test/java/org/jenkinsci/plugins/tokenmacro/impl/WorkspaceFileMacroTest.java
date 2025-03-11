package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class WorkspaceFileMacroTest {

    @Test
    void test1(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        TaskListener listener = StreamTaskListener.fromStdout();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("foo").write("Hello, world!", "UTF-8");
                return true;
            }
        });
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        WorkspaceFileMacro content = new WorkspaceFileMacro();
        content.path = "foo";
        assertEquals("Hello, world!", content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME));
        content.path = "no-such-file";
        assertEquals(
                "ERROR: File 'no-such-file' does not exist",
                content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME));
        content.fileNotFoundMessage = "No '%s' for you!";
        assertEquals("No 'no-such-file' for you!", content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME));
    }

    @Test
    void testMaxLines(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        TaskListener listener = StreamTaskListener.fromStdout();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                FilePath file = build.getWorkspace().child("foo");
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file.write()))) {
                    for (int i = 0; i < 1000; i++) {
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
        for (int i = 0; i < 10; i++) {
            assertEquals("Hello, world! " + i, lines[i]);
        }
    }

    @Test
    void testUtfEncodings(JenkinsRule j) throws Exception {
        String[] expected = {
            "première is first", "première is slightly different", "Кириллица is Cyrillic", "\uD801\uDC00 am Deseret"
        };

        String[] encodings = {"utf8", "utf16"};

        for (String encoding : encodings) {
            FreeStyleProject project = j.createFreeStyleProject();
            TaskListener listener = StreamTaskListener.fromStdout();
            project.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                        throws InterruptedException, IOException {
                    FilePath file = build.getWorkspace().child(encoding + ".txt");
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file.write(), encoding))) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(getClass().getResourceAsStream(encoding + ".txt"), encoding))) {
                            IOUtils.copy(reader, writer);
                        }
                    }
                    return true;
                }
            });
            FreeStyleBuild build = project.scheduleBuild2(0).get();

            WorkspaceFileMacro content = new WorkspaceFileMacro();
            content.path = encoding + ".txt";
            content.charSet = encoding;

            String output = content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME);
            String[] lines = output.split("\n");
            assertEquals(4, lines.length);

            for (int i = 0; i < 4; i++) {
                assertEquals(expected[i], lines[i]);
            }
        }
    }

    @Test
    void testSimpleChinese(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        TaskListener listener = StreamTaskListener.fromStdout();

        String expected;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getClass().getResourceAsStream("Chinese-Simplified.txt"), StandardCharsets.UTF_16))) {
            expected = IOUtils.toString(reader);
        }
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                FilePath file = build.getWorkspace().child("Chinese-Simplified.txt");
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file.write(), StandardCharsets.UTF_16))) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(getClass().getResourceAsStream("Chinese-Simplified.txt"), StandardCharsets.UTF_16))) {
                        IOUtils.copy(reader, writer);
                    }
                }
                return true;
            }
        });
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        WorkspaceFileMacro content = new WorkspaceFileMacro();
        content.path = "Chinese-Simplified.txt";
        content.charSet = "utf16";

        String output = content.evaluate(build, listener, WorkspaceFileMacro.MACRO_NAME);
        assertEquals(expected, output);
    }
}
