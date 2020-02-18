package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.LinkedList;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Created by acearl on 12/1/2015.
 */
public class BuildLogMacroTest {
    private static final List<String> testLog = new LinkedList<String>() {
            {
                add("line 1");
                add("line 2");
                add("line 3");
            }
        };

    private AbstractBuild build;
    private TaskListener listener;
    private BuildLogMacro buildLogMacro;

    @Before
    public void setup() {
        build = mock(AbstractBuild.class);
        listener = StreamTaskListener.fromStdout();
        buildLogMacro = new BuildLogMacro();
    }

    @Test
    public void testGetContent_shouldConcatLogWithoutLineLimit()
            throws Exception {
        when(build.getLog(anyInt())).thenReturn(testLog);

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals(String.join("\n", testLog)+"\n", content);
    }

    @Test
    public void testGetContent_shouldTruncateWhenLineLimitIsHit()
            throws Exception {
        buildLogMacro.maxLines = 2;
        buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        verify(build).getLog(2);
    }

    @Test
    public void testGetContent_shouldDefaultToMaxLines()
            throws Exception {
        buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        verify(build).getLog(BuildLogMacro.MAX_LINES_DEFAULT_VALUE);
    }

    @Test
    public void testGetContent_shouldDefaultToNotEscapeHtml()
            throws Exception {
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>() {
            {
                add("<b>bold</b>");
            }
        });

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("<b>bold</b>\n", content);
    }

    @Test
    public void testGetContent_shouldEscapeHtmlWhenArgumentEscapeHtmlSetToTrue()
            throws Exception {
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>() {
            {
                add("<b>bold</b>");
            }
        });

        buildLogMacro.escapeHtml = true;
        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("&lt;b&gt;bold&lt;/b&gt;\n", content);
    }

    @Test(expected=MacroEvaluationException.class)
    public void shouldRaiseException_on_zeroMaxLines()
            throws Exception {
        buildLogMacro.maxLines = 0;
        buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);
    }

    @Test(expected=MacroEvaluationException.class)
    public void shouldRaiseException_on_negativeMaxLines()
            throws Exception {
        buildLogMacro.maxLines = -1;
        buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);
    }

    @Test(expected=MacroEvaluationException.class)
    public void shouldRaiseException_on_negativeTruncTailLines()
            throws Exception {
        buildLogMacro.truncTailLines = -1;
        buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);
    }

    @Test
    public void shouldTruncateLog_on_nonzero_truncTailLines()
            throws Exception {
        buildLogMacro.truncTailLines = 1;
        when(build.getLog(anyInt())).thenReturn(testLog);

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals(String.join("\n", testLog.subList(0, 2))+"\n", content);
    }

    @Test
    public void shouldTruncateFullLog_on_large_truncTailLines()
            throws Exception {
        buildLogMacro.truncTailLines = 10;
        when(build.getLog(anyInt())).thenReturn(testLog);

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("", content);
    }
}
