package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class BuildUserMacroTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @WithoutJenkins
    public void testGetContent_BuildUser()
            throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        Cause.UserIdCause cause = mock(Cause.UserIdCause.class);
        when(cause.getUserId()).thenReturn("johndoe");
        when(build.getCause(Cause.UserIdCause.class)).thenReturn(cause);

        String content = new BuildUserMacro().evaluate(build, StreamTaskListener.fromStdout(), BuildUserMacro.MACRO_NAME, null, null);

        assertEquals("johndoe", content);
    }
}
