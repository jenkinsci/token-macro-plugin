package org.jenkinsci.plugins.tokenmacro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ListMultimap;
import hudson.Launcher;
import hudson.model.*;
import hudson.util.StreamTaskListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.*;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class TokenMacroTest {
    private StreamTaskListener listener;

    @Test
    void testBasics(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals(j.jenkins.getRootUrl() + "job/foo/1/", TokenMacro.expand(b, listener, "${BUILD_URL}"));
        assertEquals(j.jenkins.getRootUrl() + "job/foo/1/", TokenMacro.expand(b, listener, "$BUILD_URL"));

        assertEquals("{abc=[def, ghi], jkl=[true]}", TokenMacro.expand(b, listener, "${TEST,abc=\"def\",abc=\"ghi\",jkl=true}"));
    }

    @Test
    void testLength(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals(String.valueOf((j.jenkins.getRootUrl() + "job/foo/1/").length()), TokenMacro.expand(b, listener, "${#BUILD_URL}"));

        assertEquals(String.valueOf("{abc=[def, ghi], jkl=[true]}".length()), TokenMacro.expand(b, listener, "${#TEST,abc=\"def\",abc=\"ghi\",jkl=true}"));
    }

    @Test
    void testNested(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();

        assertEquals("{abc=[def, ghi], jkl=[true]}", TokenMacro.expand(b, listener, "${TEST_NESTED}"));
    }

    @Test
    void testVeryLongStringArg(JenkinsRule j) throws Exception {
        StringBuilder veryLongStringParam = new StringBuilder();
	    veryLongStringParam.append("abc123 %_= ~".repeat(500));

        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();

        assertEquals("{arg=[" + veryLongStringParam + "]}", TokenMacro.expand(b, listener, "${TEST, arg=\"" + veryLongStringParam + "\"}"));
    }

    @Test
    void testMultilineStringArg(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();

        assertEquals("{arg=[a \n b  \r\n c]}\n", TokenMacro.expand(b, listener, "${TEST, arg = \"a \\\n b  \\\r\\\n c\"}\n"));
    }

    @Test
    void testInvalidMultilineStringArg(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        listener = StreamTaskListener.fromStdout();
        assertThrows(MacroEvaluationException.class, () ->
            TokenMacro.expand(b, listener, "${TEST, arg = \"a \n b  \r\n c\"}\n"));
    }

    @Test
    void testEscaped(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("${TEST_NESTED}", TokenMacro.expand(b, TaskListener.NULL, "$${TEST_NESTED}"));
        assertEquals("$TEST_NESTED", TokenMacro.expand(b, TaskListener.NULL, "$$TEST_NESTED"));
        assertEquals("$TEST_NESTED{abc=[def, ghi], jkl=[true]}", TokenMacro.expand(b, TaskListener.NULL, "$$TEST_NESTED$TEST_NESTED"));
        assertEquals("${TEST_NESTED}{abc=[def, ghi], jkl=[true]}", TokenMacro.expand(b, TaskListener.NULL, "$${TEST_NESTED}$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}$TEST_NESTED", TokenMacro.expand(b, TaskListener.NULL, "$TEST_NESTED$$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}${TEST_NESTED}", TokenMacro.expand(b, TaskListener.NULL, "$TEST_NESTED$${TEST_NESTED}"));
    }

    @Test
    @Issue("JENKINS-29816")
    void testEscapedExpandAll(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("${TEST_NESTED}", TokenMacro.expandAll(b, TaskListener.NULL, "$${TEST_NESTED}"));
        assertEquals("$TEST_NESTED", TokenMacro.expandAll(b, TaskListener.NULL, "$$TEST_NESTED"));
        assertEquals("$TEST_NESTED{abc=[def, ghi], jkl=[true]}", TokenMacro.expandAll(b, TaskListener.NULL, "$$TEST_NESTED$TEST_NESTED"));
        assertEquals("${TEST_NESTED}{abc=[def, ghi], jkl=[true]}", TokenMacro.expandAll(b, TaskListener.NULL, "$${TEST_NESTED}$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}$TEST_NESTED", TokenMacro.expandAll(b, TaskListener.NULL, "$TEST_NESTED$$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}${TEST_NESTED}", TokenMacro.expandAll(b, TaskListener.NULL, "$TEST_NESTED$${TEST_NESTED}"));
    }

    @Test
    void testInvalidNoException(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("[Error replacing 'ENV' - Undefined parameter Var in token ENV]", TokenMacro.expand(b, TaskListener.NULL, "${ENV, Var=\"HOSTNAME\"}", false, Collections.EMPTY_LIST));
    }

    @Test
    void testNumeric(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!", TokenMacro.expand(
                b, TaskListener.NULL, "For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!"));
    }

    @Issue("JENKINS-18014")
    @Test
    void testEscapeCharEscaped(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals("\\{abc=[def, ghi], jkl=[true]}", TokenMacro.expand(b, listener, "\\${TEST_NESTED}"));
        assertEquals("\\{abc=[def, ghi], jkl=[true]}", TokenMacro.expand(b, listener, "\\$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}\\{abc=[def, ghi], jkl=[true]}", TokenMacro.expand(b, listener, "$TEST_NESTED\\$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}\\{abc=[def, ghi], jkl=[true]}", TokenMacro.expand(b, listener, "$TEST_NESTED\\${TEST_NESTED}"));
    }

    @Test
    void testPrivate(JenkinsRule j) throws Exception {
        List<TokenMacro> privateMacros = new ArrayList<>();
        privateMacros.add(new PrivateTestMacro());
        privateMacros.add(new PrivateTestMacro2());

        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals("TEST_PRIVATE" + j.jenkins.getRootUrl() + "job/foo/1/TEST2_PRIVATE", TokenMacro.expand(b, listener, "${TEST_PRIVATE}${BUILD_URL}${TEST2_PRIVATE}", true, privateMacros));
    }

    @Test
    void testException(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();

        assertThrows(MacroEvaluationException.class, () -> TokenMacro.expand(b, listener, "${TEST_NESTEDX}"));

        assertEquals(" ${TEST_NESTEDX}", TokenMacro.expand(b, listener, " ${TEST_NESTEDX}", false, null));
        assertEquals("${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}", TokenMacro.expand(b, listener, "${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}", false, null));
    }

    @Test
    @Issue("JENKINS-38420")
    void testJENKINS_38420(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                listener.getLogger()
                        .println("<span class=\"timestamp\"><b>14:58:18</b> </span>version: 1.0.0-SNAPSHOT");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        final String result = TokenMacro.expand(
                b,
                listener,
                "${BUILD_LOG_REGEX, regex=\"^.*?version: (.*?)$\", substText=\"$1\", maxMatches=1, showTruncatedLines=false }",
                false,
                null);
        assertEquals("1.0.0-SNAPSHOT\n", result);
    }

    @Test
    void testAutoComplete(JenkinsRule j) {
        List<String> suggestions = TokenMacro.getAutoCompleteList("LKJLKJ");
        assertEquals(0, suggestions.size());

        suggestions = TokenMacro.getAutoCompleteList("TES");
        assertEquals(3, suggestions.size());
    }

    @Test
    void testThatATokenMacroListWithANullEntryDoesNotExplode(JenkinsRule j) throws Exception {
        List<TokenMacro> badList = new LinkedList<>();
        badList.add(null);
        badList.add(new PrivateTestMacro());
        badList.add(new PrivateTestMacro2());

        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals("TEST_PRIVATE" + j.jenkins.getRootUrl() + "job/foo/1/TEST2_PRIVATE", TokenMacro.expand(b, listener, "${TEST_PRIVATE}${BUILD_URL}${TEST2_PRIVATE}", true, badList));
    }

    @Test
    @Issue("JENKINS-67862")
    void testNoToken(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        assertEquals("^(?:.*/)?master$|^(?:.*/)?(feature|release)/.* ", TokenMacro.expandAll(b, TaskListener.NULL, "^(?:.*/)?master$|^(?:.*/)?(feature|release)/.* "));

        p = j.createFreeStyleProject("foo2");
        b = p.scheduleBuild2(0).get();
        assertEquals("^false$", TokenMacro.expandAll(b, TaskListener.NULL, "^false$"));
    }

    @Test
    @Issue("JENKINS-68219")
    void testAddedCharacter(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals("\"$hello/$dear\"", TokenMacro.expand(b, listener, "\"$hello/$dear\"", false, Collections.EMPTY_LIST));
    }

    private static class PrivateTestMacro extends TokenMacro {
        private static final String MACRO_NAME = "TEST_PRIVATE";

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
                ListMultimap<String, String> argumentMultimap) {
            return "TEST_PRIVATE";
        }
    }

    private static class PrivateTestMacro2 extends TokenMacro {
        private static final String MACRO_NAME = "TEST2_PRIVATE";

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
                ListMultimap<String, String> argumentMultimap) {
            return "TEST2_PRIVATE";
        }
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
                ListMultimap<String, String> argumentMultimap) {
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
                ListMultimap<String, String> argumentMultimap) {
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
                ListMultimap<String, String> argumentMultimap) {
            return argumentMultimap.toString();
        }
    }
}
