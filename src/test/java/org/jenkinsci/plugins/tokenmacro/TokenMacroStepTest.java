package org.jenkinsci.plugins.tokenmacro;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Created by acearl on 4/10/2017.
 */
@WithJenkins
class TokenMacroStepTest {

    @Test
    void basics(JenkinsRule j) throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node { def val = tm('${BUILD_NUMBER}') ; echo val }", true));
        j.assertLogContains("1", j.assertBuildStatusSuccess(p.scheduleBuild2(0)));
    }
}
