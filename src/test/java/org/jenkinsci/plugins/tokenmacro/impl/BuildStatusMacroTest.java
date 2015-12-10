package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.AbstractBuild;
import hudson.util.StreamTaskListener;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class BuildStatusMacroTest {

    @Test
    @Issue("JENKINS-953")
    public void testGetContent_whenBuildIsBuildingThenStatusShouldBeBuilding()
            throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.isBuilding()).thenReturn(true);

        String content = new BuildStatusMacro().evaluate(build, StreamTaskListener.fromStdout(), BuildStatusMacro.MACRO_NAME);

        assertEquals("Building", content);
    }
}
