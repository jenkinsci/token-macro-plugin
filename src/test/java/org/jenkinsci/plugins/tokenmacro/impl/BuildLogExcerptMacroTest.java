package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.StringReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildLogExcerptMacroTest {

    private BuildLogExcerptMacro buildLogExcerptMacro;
    private TaskListener listener;

    @BeforeEach
    void beforeTest() {
        buildLogExcerptMacro = new BuildLogExcerptMacro();
        listener = StreamTaskListener.fromStdout();
    }

    @Test
    void testGetContent_emptyBuildLogShouldStayEmpty() throws Exception {

        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getLogReader()).thenReturn(new StringReader(""));

        buildLogExcerptMacro.start = "START";
        buildLogExcerptMacro.end = "END";

        final String result = buildLogExcerptMacro.evaluate(build, listener, BuildLogExcerptMacro.MACRO_NAME);

        assertEquals("", result);
    }

    @Test
    void testGetContent_simpleStartEndTags() throws Exception {

        AbstractBuild build = mock(AbstractBuild.class);

        when(build.getLogReader()).thenReturn(new StringReader("1\n2\n3\n4\n5\nSTART\n7\n8\n9\nEND\n10\n11\n12\n"));

        buildLogExcerptMacro.start = "START";
        buildLogExcerptMacro.end = "END";

        final String result = buildLogExcerptMacro.evaluate(build, listener, BuildLogExcerptMacro.MACRO_NAME);

        assertEquals("7\n8\n9\n", result);
    }

    @Test
    void testGetContent_regexpStartEndTags() throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getLogReader())
                .thenReturn(new StringReader("1\n2\n3\n4\n5\nTEST STARTED\n7\n8\n9\nTEST STOPED\n10\n11\n12\n"));

        buildLogExcerptMacro.start = ".*START.*";
        buildLogExcerptMacro.end = ".*STOP.*";

        final String result = buildLogExcerptMacro.evaluate(build, listener, BuildLogExcerptMacro.MACRO_NAME);

        assertEquals("7\n8\n9\n", result);
    }

    @Test
    void testGetContent_regexpStartEndTagsEndBeforeStart() throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getLogReader())
                .thenReturn(new StringReader("1\n2\nSTOP3\n4\n5\nTEST STARTED\n7\n8\n9\nTEST STOPED\n10\n11\n12\n"));

        buildLogExcerptMacro.start = ".*START.*";
        buildLogExcerptMacro.end = ".*STOP.*";

        final String result = buildLogExcerptMacro.evaluate(build, listener, BuildLogExcerptMacro.MACRO_NAME);

        assertEquals("7\n8\n9\n", result);
    }
}
