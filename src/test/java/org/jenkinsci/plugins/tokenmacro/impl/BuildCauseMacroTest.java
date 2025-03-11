package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Build;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.util.LinkedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BuildCauseMacroTest {
    private BuildCauseMacro buildCauseMacro;

    private Build build;

    private TaskListener listener;

    @BeforeEach
    void setUp() {
        buildCauseMacro = new BuildCauseMacro();
        build = mock(Build.class);
        listener = StreamTaskListener.fromStdout();
    }

    @Test
    void shouldReturnNA_whenNoCauseActionIsFound() throws Exception {
        when(build.getAction(CauseAction.class)).thenReturn(null);

        assertEquals("N/A", buildCauseMacro.evaluate(build, listener, BuildCauseMacro.MACRO_NAME));
    }

    @Test
    void shouldReturnNA_whenThereIsNoCause() throws Exception {
        CauseAction causeAction = mock(CauseAction.class);
        when(build.getAction(CauseAction.class)).thenReturn(causeAction);

        assertEquals("N/A", buildCauseMacro.evaluate(build, listener, BuildCauseMacro.MACRO_NAME));
    }

    @Test
    void shouldReturnSingleCause() throws Exception {
        CauseAction causeAction = new CauseAction(new CauseStub("Cause1"));
        when(build.getAction(CauseAction.class)).thenReturn(causeAction);

        assertEquals("Cause1", buildCauseMacro.evaluate(build, listener, BuildCauseMacro.MACRO_NAME));
    }

    @Test
    void shouldReturnMultipleCausesSeperatedByCommas() throws Exception {
        CauseAction causeAction = mock(CauseAction.class);
        when(causeAction.getCauses()).thenReturn(new LinkedList<>() {
	        {
		        add(new CauseStub("Cause1"));
		        add(new CauseStub("Cause2"));
		        add(new CauseStub("Cause3"));
	        }
        });
        when(build.getAction(CauseAction.class)).thenReturn(causeAction);

        assertEquals("Cause1, Cause2, Cause3", buildCauseMacro.evaluate(build, listener, BuildCauseMacro.MACRO_NAME));
    }

    @Test
    void testCauseData() throws Exception {
        CauseAction causeAction = mock(CauseAction.class);
        final Cause.UpstreamCause upstreamCause = mock(Cause.UpstreamCause.class);

        when(upstreamCause.getUpstreamBuild()).thenReturn(3);
        when(upstreamCause.getUpstreamProject()).thenReturn("Upstream");
        when(upstreamCause.getUpstreamUrl()).thenReturn("http://localhost/jenkins/jobs/Upstream/3");

        when(causeAction.getCauses()).thenReturn(new LinkedList<>() {
	        {
		        add(upstreamCause);
	        }
        });

        when(build.getAction(CauseAction.class)).thenReturn(causeAction);
        buildCauseMacro.data = "BUILD_NUMBER";
        assertEquals("3", buildCauseMacro.evaluate(build, listener, BuildCauseMacro.MACRO_NAME));
        buildCauseMacro.data = "PROJECT_NAME";
        assertEquals("Upstream", buildCauseMacro.evaluate(build, listener, BuildCauseMacro.MACRO_NAME));
        buildCauseMacro.data = "BUILD_URL";
        assertEquals(
                "http://localhost/jenkins/jobs/Upstream/3",
                buildCauseMacro.evaluate(build, listener, BuildCauseMacro.MACRO_NAME));
    }

    private static class CauseStub extends Cause {
        private final String name;

        private CauseStub(String name) {
            this.name = name;
        }

        @Override
        public String getShortDescription() {
            return name;
        }
    }
}
