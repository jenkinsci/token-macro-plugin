package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.util.LinkedList;
import java.util.List;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Created by acearl on 12/1/2015.
 */
class BuildLogMacroTest {
    private static final List<String> testLog = new LinkedList<>() {
	    {
		    add("line 1");
		    add("line 2");
		    add("line 3");
	    }
    };

    private AbstractBuild build;
    private TaskListener listener;
    private BuildLogMacro buildLogMacro;

    @BeforeEach
    void setup() {
        build = mock(AbstractBuild.class);
        listener = StreamTaskListener.fromStdout();
        buildLogMacro = new BuildLogMacro();
    }

    @Test
    void testGetContent_shouldConcatLogWithoutLineLimit() throws Exception {
        when(build.getLog(anyInt())).thenReturn(testLog);

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals(String.join("\n", testLog) + "\n", content);
    }

    @Test
    void testGetContent_shouldTruncateWhenLineLimitIsHit() throws Exception {
        buildLogMacro.maxLines = 2;
        buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        verify(build).getLog(2);
    }

    @Test
    void testGetContent_shouldDefaultToMaxLines() throws Exception {
        buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        verify(build).getLog(BuildLogMacro.MAX_LINES_DEFAULT_VALUE);
    }

    @Test
    void testGetContent_shouldDefaultToNotEscapeHtml() throws Exception {
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>() {
            {
                add("<b>bold</b>");
            }
        });

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("<b>bold</b>\n", content);
    }

    @Test
    void testGetContent_shouldEscapeHtmlWhenArgumentEscapeHtmlSetToTrue() throws Exception {
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>() {
            {
                add("<b>bold</b>");
            }
        });

        buildLogMacro.escapeHtml = true;
        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("&lt;b&gt;bold&lt;/b&gt;\n", content);
    }

    @Test
    void shouldRaiseException_on_zeroMaxLines() {
        buildLogMacro.maxLines = 0;
        assertThrows(MacroEvaluationException.class, () ->
            buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME));
    }

    @Test
    void shouldRaiseException_on_negativeMaxLines() {
        buildLogMacro.maxLines = -1;
        assertThrows(MacroEvaluationException.class, () ->
            buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME));
    }

    @Test
    void shouldRaiseException_on_negativeTruncTailLines() {
        buildLogMacro.truncTailLines = -1;
        assertThrows(MacroEvaluationException.class, () ->
            buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME));
    }

    @Test
    void shouldTruncateLog_on_nonzero_truncTailLines() throws Exception {
        buildLogMacro.truncTailLines = 1;
        when(build.getLog(anyInt())).thenReturn(testLog);

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals(String.join("\n", testLog.subList(0, 2)) + "\n", content);
    }

    @Test
    void shouldTruncateFullLog_on_large_truncTailLines() throws Exception {
        buildLogMacro.truncTailLines = 10;
        when(build.getLog(anyInt())).thenReturn(testLog);

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("", content);
    }

    @Test
    void testGetContent_truncated_with_maxLineLength() throws Exception {
        buildLogMacro.maxLineLength = 4;
        when(build.getLog(anyInt())).thenReturn(testLog);

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("line...\nline...\nline...\n", content);
    }

    @Test
    void testGetContent_untruncated_with_maxLineLength() throws Exception {
        buildLogMacro.maxLineLength = 6;
        when(build.getLog(anyInt())).thenReturn(testLog);

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("line 1\nline 2\nline 3\n", content);
    }
}
