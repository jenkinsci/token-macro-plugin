package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildLogMultilineRegexMacroTest {

    private BuildLogMultilineRegexMacro buildLogMultilineRegexMacro;
    private AbstractBuild build;
    private TaskListener listener;

    @BeforeEach
    void beforeTest() {
        buildLogMultilineRegexMacro = new BuildLogMultilineRegexMacro();
        buildLogMultilineRegexMacro.regex = ".+";
        build = mock(AbstractBuild.class);
        listener = StreamTaskListener.fromStdout();
    }

    @Test
    void testGetContent_multilineDotallRegex() throws Exception {
        when(build.getLogReader())
                .thenReturn(new StringReader("line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
        buildLogMultilineRegexMacro.regex = "(?s)start:.*end\\.";
        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);
        assertEquals("[...truncated 2 lines...]\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n", result);
    }

    @Test
    void testGetContent_multilineDotallRegex2() throws Exception {
        when(build.getLogReader())
                .thenReturn(new StringReader("line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
        buildLogMultilineRegexMacro.regex = "rt:(?s:.*)en";
        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);
        assertEquals("[...truncated 2 lines...]\nrt:\r\na\r\nb\r\nc\r\nen\n[...truncated 4 lines...]\n", result);
    }

    @Test
    void testGetContent_multilineEOLRegex() throws Exception {
        when(build.getLogReader())
                .thenReturn(new StringReader("line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
        buildLogMultilineRegexMacro.regex = "start:\\r?\\n(.*\\r?\\n)+end\\.";
        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);
        assertEquals("[...truncated 2 lines...]\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n", result);
    }

    @Test
    void testGetContent_multilineCommentsAlternationsRegex() throws Exception {
        when(build.getLogReader())
                .thenReturn(new StringReader("line #1\r\nline #2\r\nstart:\r\na\r\nb\r\nc\r\nend.\r\nd\r\ne\r\nf\r\n"));
        buildLogMultilineRegexMacro.regex = """
                (?x)
                # first alternative
                line\\ \\#1(?s:.*)\\#2
                # second alternative
                |start:(?s:.*)end\\.\
                # third alternative\
                |xyz(?s:.*)omega""";
        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);
        assertEquals("line #1\r\nline #2\nstart:\r\na\r\nb\r\nc\r\nend.\n[...truncated 3 lines...]\n", result);
    }

    @Test
    void testGetContent_emptyBuildLogShouldStayEmpty() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(""));
        final BufferedReader reader = new BufferedReader(new StringReader(""));
        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);
        assertEquals("", result);
    }

    @Test
    void testGetContent_matchedLines() throws Exception {
        when(build.getLogReader())
                .thenReturn(
                        new StringReader(
                                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
        buildLogMultilineRegexMacro.regex = ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*";
        buildLogMultilineRegexMacro.showTruncatedLines = false;
        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);
        assertEquals("6 ERROR\n9 ERROR\n18 ERROR\n", result);
    }

    @Test
    void testGetContent_truncatedAndMatchedLines() throws Exception {
        when(build.getLogReader())
                .thenReturn(
                        new StringReader(
                                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));

        buildLogMultilineRegexMacro.regex = ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*";
        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals(
                "[...truncated 5 lines...]\n6 ERROR\n[...truncated 2 lines...]\n9 ERROR\n[...truncated 8 lines...]\n18 ERROR\n[...truncated 5 lines...]\n",
                result);
    }

    @Test
    void testGetContent_errorMatchedAndNothingReplaced() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar fubber"));
        buildLogMultilineRegexMacro.substText = "$0";

        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals("error foo bar fubber\n", result);
    }

    @Test
    void testGetContent_errorMatchedAndNothingReplaced2() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar fubber"));
        buildLogMultilineRegexMacro.substText = null;

        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals("error foo bar fubber\n", result);
    }

    @Test
    void testGetContent_errorMatchedAndReplacedByString() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar error fubber"));
        buildLogMultilineRegexMacro.regex = ".*(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b.*";
        buildLogMultilineRegexMacro.substText = "REPLACE";
        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals("REPLACE\n", result);
    }

    @Test
    void testGetContent_prefixMatchedTruncatedAndStripped() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("prefix: Yes\nRandom Line\nprefix: No\n"));
        buildLogMultilineRegexMacro.regex = "(?:^|(?<=\n))prefix: ((?-s:.*))(?:$|(?=[\r\n]))";
        buildLogMultilineRegexMacro.showTruncatedLines = false;
        buildLogMultilineRegexMacro.substText = "$1";

        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals("Yes\nNo\n", result);
    }

    @Test
    void testGetContent_escapeHtml() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error <>&\""));
        buildLogMultilineRegexMacro.showTruncatedLines = false;
        buildLogMultilineRegexMacro.escapeHtml = true;

        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals("error &lt;&gt;&amp;&quot;\n", result);
    }

    @Test
    void testGetContent_matchedSegmentHtmlStyleEmpty() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error"));
        buildLogMultilineRegexMacro.showTruncatedLines = false;
        buildLogMultilineRegexMacro.matchedSegmentHtmlStyle = "";

        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals("<pre>\n<b>error</b>\n</pre>\n", result);
    }

    @Test
    void testGetContent_matchedSegmentHtmlStyle() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error"));
        buildLogMultilineRegexMacro.showTruncatedLines = false;
        buildLogMultilineRegexMacro.matchedSegmentHtmlStyle = "color: red";

        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals("<pre>\n<b style=\"color: red\">error</b>\n</pre>\n", result);
    }

    @Test
    void testGetContent_shouldStripOutConsoleNotes() throws Exception {
        // See HUDSON-7402
        buildLogMultilineRegexMacro.regex = ".+";
        buildLogMultilineRegexMacro.showTruncatedLines = false;
        when(build.getLogReader())
                .thenReturn(new StringReader(ConsoleNote.PREAMBLE_STR
                        + "AAAAdB+LCAAAAAAAAABb85aBtbiIQSOjNKU4P0+vIKc0PTOvWK8kMze1uCQxtyC1SC8ExvbLL0llgABGJgZGLwaB3MycnMzi4My85FTXgvzkjIoiBimoScn5ecX5Oal6zhAaVS9DRQGQ1uaZsmc5AAaMIAyBAAAA"
                        + ConsoleNote.POSTAMBLE_STR + "No emails were triggered."));

        final String result =
                buildLogMultilineRegexMacro.evaluate(build, listener, BuildLogMultilineRegexMacro.MACRO_NAME);

        assertEquals("No emails were triggered.\n", result);
    }
}
