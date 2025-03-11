package org.jenkinsci.plugins.tokenmacro.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.util.StreamTaskListener;
import java.io.StringWriter;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class BuildStatusMacroTest {

    @Test
    @Issue("JENKINS-953")
    @WithoutJenkins
    void testGetContent_whenBuildIsBuildingThenStatusShouldBeBuilding(JenkinsRule j) throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.isBuilding()).thenReturn(true);

        String content =
                new BuildStatusMacro().evaluate(build, StreamTaskListener.fromStdout(), BuildStatusMacro.MACRO_NAME);

        assertEquals("Building", content);
    }

    @Test
    @Issue("JENKINS-44322")
    void testGetContent_whenPipelineBuildIsBuildingThenStatusShouldBeSuccess(JenkinsRule j) throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("node { echo tm('$BUILD_STATUS') }", false));
        WorkflowRun run = job.scheduleBuild2(0).get();

        j.assertBuildStatus(Result.SUCCESS, run);
        StringWriter w = new StringWriter();
        run.getLogText().writeLogTo(0, w);
        assertTrue(w.toString().contains(Result.SUCCESS.toString()));
    }
}
