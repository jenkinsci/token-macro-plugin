package org.jenkinsci.plugins.tokenmacro;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.ListMultimap;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Label;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.slaves.DumbSlave;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

/**
 * Created by acearl on 6/14/2016.
 */
public class PipelineTest {
    private StreamTaskListener listener;

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    private DumbSlave agent;

    @Before
    public void setup() throws Exception {
        agent = j.createOnlineSlave(Label.get("agents"));
    }

    @Test
    public void testEnvironmentVariables() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition(getPipeline("any", "${ENV, var=\"VERSION\"}"), true));
        Run<?, ?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        j.assertLogContains("VERSION=1.0.0", run);
    }

    @Test
    public void testEnvironmentVariablesNoAgent() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition(getPipeline("none", "${ENV, var=\"VERSION\"}"), true));
        Run<?, ?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        j.assertLogContains("VERSION=1.0.0", run);
    }

    @Test
    public void testWorkspaceNeededNoAgent() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition(getPipeline("none", "${TEST_WS}"), true));
        Run<?, ?> run = j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0));
        j.assertLogContains("Macro 'TEST_WS' can ony be evaluated in a workspace", run);
    }

    @Test
    public void testWorkspaceNeededWithAgent() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition(getPipeline("any", "${TEST_WS}"), true));
        Run<?, ?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        j.assertLogContains("Workspace: foo", run);
    }

    @Test
    public void testFileNeededWithAgent() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        FilePath workspace = agent.getWorkspaceFor(job);
        workspace.mkdirs();
        try (OutputStream of = new FilePath(workspace, "in.txt").write()) {
            of.write("42".getBytes(StandardCharsets.UTF_8));
        }
        job.setDefinition(new CpsFlowDefinition(getPipeline("{label 'agents'}", "${FILE,path=\"in.txt\"}"), true));
        Run<?, ?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        j.assertLogContains("VERSION=42", run);
    }

    @Test
    public void testEscapedExpandAll() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");

        job.setDefinition(new CpsFlowDefinition("node('agents') {\n\techo 'Hello, world'\n}", true));
        Run<?, ?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        listener = StreamTaskListener.fromStdout();
        assertEquals(j.jenkins.getRootUrl() + "job/foo/1/", TokenMacro.expand(run, null, listener, "${BUILD_URL}"));
        assertEquals(j.jenkins.getRootUrl() + "job/foo/1/", TokenMacro.expand(run, null, listener, "$BUILD_URL"));

        assertEquals(
                "{abc=[def, ghi], jkl=[true]}",
                TokenMacro.expand(run, null, listener, "${TEST,abc=\"def\",abc=\"ghi\",jkl=true}"));
    }

    @Test
    public void testException() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition("node('agents') {\n\techo 'Hello, world'\n}", true));
        Run<?, ?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        listener = StreamTaskListener.fromStdout();

        try {
            TokenMacro.expand(run, null, listener, "${TEST_NESTEDX}");
            fail();
        } catch (MacroEvaluationException e) {
            // do nothing, just want to catch the exception when it occurs
        }

        assertEquals(" ${TEST_NESTEDX}", TokenMacro.expand(run, null, listener, " ${TEST_NESTEDX}", false, null));
        assertEquals(
                "${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}",
                TokenMacro.expand(
                        run, null, listener, "${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}", false, null));
    }

    @Test
    public void testUnconvertedMacro() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "foo");
        job.setDefinition(new CpsFlowDefinition("node('agents') {\n\techo 'Hello, world'\n}", true));
        Run<?, ?> run = j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        listener = StreamTaskListener.fromStdout();

        assertEquals("TEST_2 is not supported in this context", TokenMacro.expand(run, null, listener, "${TEST_2}"));
    }

    private String getPipeline(String agent, String macro) {
        return "pipeline {\n" + "  agent "
                + agent + "\n" + "  environment {\n"
                + "    VERSION = \"1.0.0\"\n"
                + "  }\n\n"
                + "  stages {\n"
                + "    stage('Cool') {\n"
                + "      steps {\n"
                + "        echo \"VERSION=\" + tm('"
                + macro + "')\n" + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}";
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
        public String evaluate(
                AbstractBuild<?, ?> context,
                TaskListener listener,
                String macroName,
                Map<String, String> arguments,
                ListMultimap<String, String> argumentMultimap)
                throws MacroEvaluationException, IOException, InterruptedException {
            return evaluate(context, null, listener, macroName, arguments, argumentMultimap);
        }

        @Override
        public String evaluate(
                Run<?, ?> run,
                FilePath workspace,
                TaskListener listener,
                String macroName,
                Map<String, String> arguments,
                ListMultimap<String, String> argumentMultimap)
                throws MacroEvaluationException, IOException, InterruptedException {
            return argumentMultimap.toString();
        }
    }

    @TestExtension
    public static class NestedTestMacro extends TokenMacro {
        private static final String MACRO_NAME = "TEST_NESTED";

        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals(MACRO_NAME);
        }

        @Override
        public String evaluate(
                AbstractBuild<?, ?> context,
                TaskListener listener,
                String macroName,
                Map<String, String> arguments,
                ListMultimap<String, String> argumentMultimap)
                throws MacroEvaluationException, IOException, InterruptedException {
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
        public String evaluate(
                AbstractBuild<?, ?> context,
                TaskListener listener,
                String macroName,
                Map<String, String> arguments,
                ListMultimap<String, String> argumentMultimap)
                throws MacroEvaluationException, IOException, InterruptedException {
            return argumentMultimap.toString();
        }
    }

    @TestExtension
    public static class TestMacroWS extends WorkspaceDependentMacro {
        private static final String MACRO_NAME = "TEST_WS";

        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals(MACRO_NAME);
        }

        @Override
        public List<String> getAcceptedMacroNames() {
            return Collections.singletonList(MACRO_NAME);
        }

        @Override
        public String evaluate(
                AbstractBuild<?, ?> context,
                TaskListener listener,
                String macroName,
                Map<String, String> arguments,
                ListMultimap<String, String> argumentMultimap)
                throws MacroEvaluationException, IOException, InterruptedException {
            return evaluate(context, context.getWorkspace(), listener, macroName, arguments, argumentMultimap);
        }

        @Override
        public Callable<String, IOException> getCallable(Run<?, ?> run, String root, TaskListener listener) {
            return new MasterToSlaveCallable<String, IOException>() {
                @Override
                public String call() throws IOException {
                    return "Workspace: " + new File(root).getName();
                }
            };
        }
    }
}
