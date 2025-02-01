package org.jenkinsci.plugins.tokenmacro;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Created by acearl on 4/10/2017.
 */
public class TokenMacroStepTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void basics() throws Exception {
        WorkflowJob p = j.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node { def val = tm('${BUILD_NUMBER}') ; echo val }", true));
        j.assertLogContains("1", j.assertBuildStatusSuccess(p.scheduleBuild2(0)));
    }
}
