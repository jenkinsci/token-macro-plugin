package org.jenkinsci.plugins.tokenmacro;

import com.google.common.collect.ListMultimap;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Label;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by acearl on 6/14/2016.
 */
public class PipelineTest {
    private StreamTaskListener listener;

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Before
    public void setup() throws Exception {
	    j.createOnlineSlave(Label.get("agents"));
    }

    @Test
    public void testEscapedExpandAll() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");

        job.setDefinition(new CpsFlowDefinition("node('agents') {\n\techo 'Hello, world'\n}", true));
        Run<?,?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        listener = StreamTaskListener.fromStdout();
        assertEquals(j.jenkins.getRootUrl()+"job/foo/1/",TokenMacro.expand(run,null,listener,"${BUILD_URL}"));
        assertEquals(j.jenkins.getRootUrl()+"job/foo/1/",TokenMacro.expand(run,null,listener,"$BUILD_URL"));

        assertEquals("{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(run,null,listener,"${TEST,abc=\"def\",abc=\"ghi\",jkl=true}"));
    }

    @Test
    public void testException() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition("node('agents') {\n\techo 'Hello, world'\n}", true));
        Run<?,?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        listener = StreamTaskListener.fromStdout();

        try {
            TokenMacro.expand(run,null,listener,"${TEST_NESTEDX}");
            fail();
        } catch(MacroEvaluationException e) {
            // do nothing, just want to catch the exception when it occurs
        }

        assertEquals(" ${TEST_NESTEDX}", TokenMacro.expand(run,null,listener," ${TEST_NESTEDX}",false,null));
        assertEquals("${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}", TokenMacro.expand(run,null,listener,"${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}",false,null));
    }

    @Test
    public void testUnconvertedMacro() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition("node('agents') {\n\techo 'Hello, world'\n}", true));
        Run<?,?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        listener = StreamTaskListener.fromStdout();

        assertEquals("TEST_2 is not supported in this context", TokenMacro.expand(run,null,listener,"${TEST_2}"));
    }

    @TestExtension
    public static class TestMacro extends TokenMacro {
        private static final String MACRO_NAME = "TEST";

        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals(MACRO_NAME);
        }

        @Override
        public List<String> getAcceptedMacroNames() {
            return Collections.singletonList(MACRO_NAME);
        }

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return evaluate(context,null,listener,macroName,arguments,argumentMultimap);
        }

        @Override
        public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return argumentMultimap.toString();
        }
    }

    @TestExtension
    public static class NestedTestMacro extends TokenMacro {
        private static final String MACRO_NAME = "TEST_NESTED";

        @Override
        public boolean acceptsMacroName(String macroName) { return macroName.equals(MACRO_NAME); }

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return "${TEST,abc=\"def\",abc=\"ghi\",jkl=true}";
        }

        @Override
        public List<String> getAcceptedMacroNames() {
            return Collections.singletonList(MACRO_NAME);
        }

        @Override
        public boolean hasNestedContent() {
            return true;
        }
    }

    @TestExtension
    public static class TestMacro2 extends TokenMacro {
        private static final String MACRO_NAME = "TEST_2";

        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals(MACRO_NAME);
        }

        @Override
        public List<String> getAcceptedMacroNames() {
            return Collections.singletonList(MACRO_NAME);
        }

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return argumentMultimap.toString();
        }
    }
}
