package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.common.collect.ArrayListMultimap;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

public class BuildLogRegexMacroTest {

    private BuildLogRegexMacro buildLogRegexMacro;
    private TaskListener listener;
    private AbstractBuild build;

    private final String TRUNC_1_LINE_TEXT = "[...truncated 1 lines...]\n";
    private final String TRUNC_2_LINE_TEXT = "[...truncated 2 lines...]\n";
    private final String TRUNC_1_LINE_HTML = "<p>[...truncated 1 lines...]</p>\n";
    private final String TRUNC_2_LINE_HTML = "<p>[...truncated 2 lines...]</p>\n";

    @Before
    public void beforeTest() {
        buildLogRegexMacro = new BuildLogRegexMacro();
        listener = StreamTaskListener.fromStdout();
        build = mock(AbstractBuild.class);
    }

    private void mockLogReaderWithSimpleErrorLog() throws Exception {
        when(build.getLogReader())
                .thenReturn(
                        new StringReader(
                                "1\n2\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n13\n14\n15\n16\n17\n18 ERROR\n19\n20\n21\n22\n23\n"));
    }

    @Test
    public void testGetContent_emptyBuildLogShouldStayEmpty() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(""));

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("", result);
    }

    @Test
    public void testGetContent_matchedLines() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.showTruncatedLines = false;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("6 ERROR\n9 ERROR\n18 ERROR\n", result);
    }

    @Test
    public void testGetContent_matchedLines_with_maxMatches() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.maxMatches = 1;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("6 ERROR\n", result);
    }

    @Test
    public void testGetContent_matchedLines_when_not_greedy_with_default_maxMatches() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.greedy = false;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("6 ERROR\n9 ERROR\n18 ERROR\n", result);
    }

    @Test
    public void testGetContent_matchedLines_with_maxTailMatches() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.maxTailMatches = 1;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("18 ERROR\n", result);
    }

    @Test
    public void testGetContent_matchedLines_with_maxMatches_and_maxTailMatches() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.maxMatches = 2;
        buildLogRegexMacro.maxTailMatches = 1;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("9 ERROR\n", result);
    }

    @Test
    public void testGetContent_truncatedAndMatchedLines() throws Exception {
        mockLogReaderWithSimpleErrorLog();

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(
                "[...truncated 5 lines...]\n6 ERROR\n[...truncated 2 lines...]\n9 ERROR\n[...truncated 8 lines...]\n18 ERROR\n[...truncated 5 lines...]\n",
                result);
    }

    @Test
    public void testGetContent_truncatedMatchedAndContextLines() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.linesBefore = 3;
        buildLogRegexMacro.linesAfter = 3;
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(
                "[...truncated 2 lines...]\n3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n[...truncated 2 lines...]\n15\n16\n17\n18 ERROR\n19\n20\n21\n[...truncated 2 lines...]\n",
                result);
    }

    @Test
    public void testGetContent_matchedAndContextLines() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.linesBefore = 3;
        buildLogRegexMacro.linesAfter = 3;
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("3\n4\n5\n6 ERROR\n7\n8\n9 ERROR\n10\n11\n12\n15\n16\n17\n18 ERROR\n19\n20\n21\n", result);
    }

    @Test
    public void testGetContent_truncatedMatchedAndContextLinesAsHtml() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.matchedLineHtmlStyle = "color: red";
        buildLogRegexMacro.linesBefore = 3;
        buildLogRegexMacro.linesAfter = 3;
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(
                "<p>[...truncated 2 lines...]</p>\n<pre>\n3\n4\n5\n<b style=\"color: red\">6 ERROR</b>\n7\n8\n<b style=\"color: red\">9 ERROR</b>\n10\n11\n12\n</pre>\n<p>[...truncated 2 lines...]</p>\n<pre>\n15\n16\n17\n<b style=\"color: red\">18 ERROR</b>\n19\n20\n21\n</pre>\n<p>[...truncated 2 lines...]</p>\n",
                result);
    }

    @Test
    public void testGetContent_matchedAndContextLinesAsHtml() throws Exception {
        mockLogReaderWithSimpleErrorLog();
        buildLogRegexMacro.matchedLineHtmlStyle = "color: red";
        buildLogRegexMacro.linesBefore = 3;
        buildLogRegexMacro.linesAfter = 3;
        buildLogRegexMacro.showTruncatedLines = false;
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(
                "<pre>\n3\n4\n5\n<b style=\"color: red\">6 ERROR</b>\n7\n8\n<b style=\"color: red\">9 ERROR</b>\n10\n11\n12\n15\n16\n17\n<b style=\"color: red\">18 ERROR</b>\n19\n20\n21\n</pre>\n",
                result);
    }

    @Test
    public void testGetContent_matchedLines_as_text_showing_truncated_lines() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("a\n1\nb\n2\nc\n3\n"));
        buildLogRegexMacro.regex = "\\d";
        buildLogRegexMacro.showTruncatedLines = true;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(TRUNC_1_LINE_TEXT + "1\n" + TRUNC_1_LINE_TEXT + "2\n" + TRUNC_1_LINE_TEXT + "3\n", result);
    }

    public void getContent_matchedLines_as_text_showing_truncated_lines_with_maxTailMatches(
            int maxTailMatches, String expectedResult) throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("a\n1\nb\n2\nc\n3\n"));
        buildLogRegexMacro.regex = "\\d";
        buildLogRegexMacro.showTruncatedLines = true;
        buildLogRegexMacro.maxTailMatches = maxTailMatches;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetContent_matchedLines_as_text_showing_truncated_lines_1() throws Exception {
        getContent_matchedLines_as_text_showing_truncated_lines_with_maxTailMatches(1, "3\n");
    }

    @Test
    public void testGetContent_matchedLines_as_text_showing_truncated_lines_2() throws Exception {
        getContent_matchedLines_as_text_showing_truncated_lines_with_maxTailMatches(
                2, "2\n" + TRUNC_1_LINE_TEXT + "3\n");
    }

    @Test
    public void testGetContent_matchedLines_as_text_showing_truncated_lines_3() throws Exception {
        getContent_matchedLines_as_text_showing_truncated_lines_with_maxTailMatches(
                3, "1\n" + TRUNC_1_LINE_TEXT + "2\n" + TRUNC_1_LINE_TEXT + "3\n");
    }

    @Test
    public void testGetContent_matchedLines_as_text_showing_truncated_lines_4() throws Exception {
        getContent_matchedLines_as_text_showing_truncated_lines_with_maxTailMatches(
                4, TRUNC_1_LINE_TEXT + "1\n" + TRUNC_1_LINE_TEXT + "2\n" + TRUNC_1_LINE_TEXT + "3\n");
    }

    @Test
    public void testGetContent_matchedLines_as_html_showing_truncated_lines() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("a\n1\nb\n2\nc\n3\n"));
        buildLogRegexMacro.regex = "\\d";
        buildLogRegexMacro.showTruncatedLines = true;
        buildLogRegexMacro.matchedLineHtmlStyle = "";

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(
                TRUNC_1_LINE_HTML + "<pre>\n<b>1</b>\n</pre>\n" + TRUNC_1_LINE_HTML + "<pre>\n<b>2</b>\n</pre>\n"
                        + TRUNC_1_LINE_HTML + "<pre>\n<b>3</b>\n</pre>\n",
                result);
    }

    public void getContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches(
            int maxTailMatches, String expectedResult) throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("a\n1\nb\n2\nc\n3\n"));
        buildLogRegexMacro.regex = "\\d";
        buildLogRegexMacro.showTruncatedLines = true;
        buildLogRegexMacro.matchedLineHtmlStyle = "";
        buildLogRegexMacro.maxTailMatches = maxTailMatches;
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches_1() throws Exception {
        getContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches(1, "<pre>\n<b>3</b>\n</pre>\n");
    }

    @Test
    public void testGetContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches_2() throws Exception {
        getContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches(
                2, "<pre>\n<b>2</b>\n</pre>\n" + TRUNC_1_LINE_HTML + "<pre>\n<b>3</b>\n</pre>\n");
    }

    @Test
    public void testGetContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches_3() throws Exception {
        getContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches(
                3,
                "<pre>\n<b>1</b>\n</pre>\n" + TRUNC_1_LINE_HTML + "<pre>\n<b>2</b>\n</pre>\n" + TRUNC_1_LINE_HTML
                        + "<pre>\n<b>3</b>\n</pre>\n");
    }

    @Test
    public void testGetContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches_4() throws Exception {
        getContent_matchedLines_as_html_showing_truncated_lines_with_maxTailMatches(
                4,
                TRUNC_1_LINE_HTML + "<pre>\n<b>1</b>\n</pre>\n"
                        + TRUNC_1_LINE_HTML + "<pre>\n<b>2</b>\n</pre>\n"
                        + TRUNC_1_LINE_HTML + "<pre>\n<b>3</b>\n</pre>\n");
    }

    public void testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches(
            int maxTailMatches, String expectedResult) throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("a\nb\n1\n2\nc\nd\n3\n4\ne\nf\n5\n6\n"));
        buildLogRegexMacro.regex = "\\d";
        buildLogRegexMacro.showTruncatedLines = true;
        buildLogRegexMacro.matchedLineHtmlStyle = "";
        buildLogRegexMacro.maxTailMatches = maxTailMatches;
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches_1() throws Exception {
        testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches(
                1, "<pre>\n<b>6</b>\n</pre>\n");
    }

    @Test
    public void testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches_2() throws Exception {
        testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches(
                2, "<pre>\n<b>5</b>\n<b>6</b>\n</pre>\n");
    }

    @Test
    public void testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches_3() throws Exception {
        testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches(
                3, "<pre>\n<b>4</b>\n</pre>\n" + TRUNC_2_LINE_HTML + "<pre>\n<b>5</b>\n<b>6</b>\n</pre>\n");
    }

    @Test
    public void testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches_6() throws Exception {
        testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches(
                6,
                "<pre>\n<b>1</b>\n<b>2</b>\n</pre>\n" + TRUNC_2_LINE_HTML + "<pre>\n<b>3</b>\n<b>4</b>\n</pre>\n"
                        + TRUNC_2_LINE_HTML + "<pre>\n<b>5</b>\n<b>6</b>\n</pre>\n");
    }

    @Test
    public void testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches_7() throws Exception {
        testGetContent_matchedBlocks_as_html_showing_truncated_lines_with_maxTailMatches(
                7,
                TRUNC_2_LINE_HTML + "<pre>\n<b>1</b>\n<b>2</b>\n</pre>\n"
                        + TRUNC_2_LINE_HTML + "<pre>\n<b>3</b>\n<b>4</b>\n</pre>\n"
                        + TRUNC_2_LINE_HTML + "<pre>\n<b>5</b>\n<b>6</b>\n</pre>\n");
    }

    private void testGetContent_line_truncation_with_maxLineLength(
            String input, String regex, String expectedResult, int maxLineLength) throws Exception {
        testGetContent_line_truncation_with_maxLineLength(input, regex, expectedResult, maxLineLength, 0, 0);
    }

    private void testGetContent_line_truncation_with_maxLineLength(
            String input, String regex, String expectedResult, int maxLineLength, int linesBefore, int linesAfter)
            throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader(input));
        buildLogRegexMacro.linesBefore = linesBefore;
        buildLogRegexMacro.linesAfter = linesAfter;
        buildLogRegexMacro.regex = regex;
        buildLogRegexMacro.maxLineLength = maxLineLength;
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetContent_truncated_lines_with_maxLineLength() throws Exception {
        testGetContent_line_truncation_with_maxLineLength(
                "short line\na longer line\n", ".", "short line\na longer l...\n", 10);
    }

    @Test
    public void testGetContent_truncated_context_lines_with_maxLineLength() throws Exception {
        testGetContent_line_truncation_with_maxLineLength("ab\n1\nc\n", "\\d", "a...\n1\nc\n", 1, 1, 1);
    }

    @Test
    public void testGetContent_errorMatchedAndNothingReplaced() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar fubber"));
        buildLogRegexMacro.substText = "$0";

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("error foo bar fubber\n", result);
    }

    @Test
    public void testGetContent_errorMatchedAndNothingReplaced2() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar fubber"));
        buildLogRegexMacro.substText = null;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("error foo bar fubber\n", result);
    }

    @Test
    public void testGetContent_errorMatchedAndReplacedByString() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error foo bar error fubber"));
        buildLogRegexMacro.substText = "REPLACE";

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("REPLACE foo bar REPLACE fubber\n", result);
    }

    @Test
    public void testGetContent_prefixMatchedTruncatedAndStripped() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("prefix: Yes\nRandom Line\nprefix: No\n"));
        buildLogRegexMacro.regex = "^prefix: (.*)$";
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.substText = "$1";

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("Yes\nNo\n", result);
    }

    @Test
    public void testGetContent_escapeHtml() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error <>&\""));
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.escapeHtml = true;

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("error &lt;&gt;&amp;&quot;\n", result);
    }

    @Test
    public void testGetContent_matchedLineHtmlStyleEmpty() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error"));
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.matchedLineHtmlStyle = "";

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("<pre>\n<b>error</b>\n</pre>\n", result);
    }

    @Test
    public void testGetContent_matchedLineHtmlStyle() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("error"));
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.matchedLineHtmlStyle = "color: red";

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("<pre>\n<b style=\"color: red\">error</b>\n</pre>\n", result);
    }

    @Test
    public void testGetContent_matchedLineHtmlStyleWithHtmlEscape() throws Exception {
        when(build.getLogReader()).thenReturn(new StringReader("<error>"));
        final Map<String, String> arguments = new HashMap<>();
        arguments.put("escapeHtml", "true");
        arguments.put("showTruncatedLines", "false");
        arguments.put("matchedLineHtmlStyle", "color: red");
        final ArrayListMultimap<String, String> listMultimap = ArrayListMultimap.create();
        listMultimap.put("escapeHtml", "true");
        listMultimap.put("showTruncatedLines", "false");
        listMultimap.put("matchedLineHtmlStyle", "color: red");
        final String result = buildLogRegexMacro.evaluate(
                build, null, listener, BuildLogRegexMacro.MACRO_NAME, arguments, listMultimap);

        assertEquals("<pre>\n<b style=\"color: red\">&lt;error&gt;</b>\n</pre>\n", result);
    }

    @Test
    public void testGetContent_shouldStripOutConsoleNotes() throws Exception {
        // See HUDSON-7402
        buildLogRegexMacro.regex = ".*";
        buildLogRegexMacro.showTruncatedLines = false;
        when(build.getLogReader())
                .thenReturn(new StringReader(ConsoleNote.PREAMBLE_STR
                        + "AAAAdB+LCAAAAAAAAABb85aBtbiIQSOjNKU4P0+vIKc0PTOvWK8kMze1uCQxtyC1SC8ExvbLL0llgABGJgZGLwaB3MycnMzi4My85FTXgvzkjIoiBimoScn5ecX5Oal6zhAaVS9DRQGQ1uaZsmc5AAaMIAyBAAAA"
                        + ConsoleNote.POSTAMBLE_STR + "No emails were triggered."));

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("No emails were triggered.\n", result);
    }

    @Test
    public void testGetContent_addNewLineFalse() throws Exception {
        // See JENKINS-14320
        buildLogRegexMacro.addNewline = false;
        buildLogRegexMacro.regex = "^\\*{3} Application: (.*)$";
        buildLogRegexMacro.maxMatches = 1;
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.substText = "$1";
        when(build.getLogReader())
                .thenReturn(new StringReader("*** Application: Firefox 15.0a2\n*** Platform: Mac OS X 10.7.4 64bit"));
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("Firefox 15.0a2", result);
    }

    @Test
    public void testGetContent_defaultValue() throws Exception {
        // See JENKINS-16269
        buildLogRegexMacro.defaultValue = "JENKINS";
        buildLogRegexMacro.regex = "^\\*{3} Blah Blah: (.*)$";
        buildLogRegexMacro.maxMatches = 1;
        buildLogRegexMacro.showTruncatedLines = false;
        buildLogRegexMacro.substText = "$1";
        when(build.getLogReader())
                .thenReturn(new StringReader("*** Application: Firefox 15.0a2\n*** Platform: Mac OS X 10.7.4 64bit"));
        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        assertEquals("JENKINS", result);
    }

    @Test
    @Issue("JENKINS-49746")
    public void testOverlappingResults() throws Exception {
        buildLogRegexMacro.regex = "(?m)^(nc\\w+:\\s+\\*E,|.*\\bNG\\b|\\s*Error\\b|E-).*$";
        buildLogRegexMacro.linesBefore = 5;
        buildLogRegexMacro.greedy = false;
        buildLogRegexMacro.linesAfter = 10;
        buildLogRegexMacro.maxMatches = 5;
        buildLogRegexMacro.showTruncatedLines = false;

        when(build.getLogReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("JENKINS-49746-input.txt")));

        final String result = buildLogRegexMacro.evaluate(build, listener, BuildLogRegexMacro.MACRO_NAME);

        final String expected = IOUtils.toString(getClass().getResourceAsStream("JENKINS-49746-output.txt"));
        assertEquals(
                expected.replaceAll("\r\n", "\n").trim(),
                result.replaceAll("\r\n", "\n").trim());
    }
}
