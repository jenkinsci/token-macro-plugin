package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.util.StreamTaskListener;
import org.junit.jupiter.api.Test;

class BuildUserMacroTest {

    @Test
    void testGetContent_BuildUser() throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        Cause.UserIdCause cause = mock(Cause.UserIdCause.class);
        when(cause.getUserId()).thenReturn("johndoe");
        when(build.getCause(Cause.UserIdCause.class)).thenReturn(cause);

        String content = new BuildUserMacro()
                .evaluate(build, StreamTaskListener.fromStdout(), BuildUserMacro.MACRO_NAME, null, null);

        assertEquals("johndoe", content);
    }
}
