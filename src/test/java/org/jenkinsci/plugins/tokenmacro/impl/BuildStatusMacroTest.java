package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.AbstractBuild;
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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked"})
public class BuildStatusMacroTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("JENKINS-953")
    @WithoutJenkins
    public void testGetContent_whenBuildIsBuildingThenStatusShouldBeBuilding()
            throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.isBuilding()).thenReturn(true);

        String content = new BuildStatusMacro().evaluate(build, StreamTaskListener.fromStdout(), BuildStatusMacro.MACRO_NAME);

        assertEquals("Building", content);
    }

    @Test
    @Issue("JENKINS-44322")
    public void testGetContent_whenPipelineBuildIsBuildingThenStatusShouldBeSuccess() throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("node { echo tm('$BUILD_STATUS') }", false));
        WorkflowRun run = job.scheduleBuild2(0).get();

        j.assertBuildStatus(Result.SUCCESS, run);
        StringWriter w = new StringWriter();
        run.getLogText().writeLogTo(0, w);
        assertTrue(w.toString().contains(Result.SUCCESS.toString()));
    }
}
