package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Created by acearl on 12/1/2015.
 */
public class BuildLogMacroTest {
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
        when(build.getLog(anyInt())).thenReturn(new LinkedList<String>() {
            {
                add("line 1");
                add("line 2");
                add("line 3");
            }
        });

        String content = buildLogMacro.evaluate(build, listener, BuildLogMacro.MACRO_NAME);

        assertEquals("line 1\nline 2\nline 3\n", content);
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
}
