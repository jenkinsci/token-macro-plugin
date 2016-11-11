package org.jenkinsci.plugins.tokenmacro;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import hudson.Launcher;
import hudson.model.*;
import hudson.util.StreamTaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;

/**
 * @author Kohsuke Kawaguchi
 */
public class TokenMacroTest {
    private StreamTaskListener listener;
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testBasics() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals(j.jenkins.getRootUrl()+"job/foo/1/",TokenMacro.expand(b, listener,"${BUILD_URL}"));
        assertEquals(j.jenkins.getRootUrl()+"job/foo/1/",TokenMacro.expand(b, listener,"$BUILD_URL"));

        assertEquals("{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"${TEST,abc=\"def\",abc=\"ghi\",jkl=true}"));
    }

    @Test
    public void testLength() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals(String.valueOf((j.jenkins.getRootUrl()+"job/foo/1/").length()),TokenMacro.expand(b, listener,"${#BUILD_URL}"));

        assertEquals(String.valueOf("{abc=[def, ghi], jkl=[true]}".length()),TokenMacro.expand(b,listener,"${#TEST,abc=\"def\",abc=\"ghi\",jkl=true}"));
    }

    @Test
    public void testNested() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();

        assertEquals("{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"${TEST_NESTED}"));
    }

    @Test
    public void testVeryLongStringArg() throws Exception {
        StringBuilder veryLongStringParam = new StringBuilder();
        for (int i = 0 ; i < 500 ; ++i) {
            veryLongStringParam.append("abc123 %_= ~");
        }

        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();

        assertEquals("{arg=["+ veryLongStringParam + "]}",TokenMacro.expand(b,listener,"${TEST, arg=\"" + veryLongStringParam + "\"}"));
    }

    @Test
    public void testMultilineStringArgs() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();

        assertEquals("{arg=[a \n b  \r\n c]}\n",TokenMacro.expand(b, listener, "${TEST, arg = \"a \\\n b  \\\r\n c\"}\n"));

        assertEquals("${TEST, arg = \"a \n b  \r\n c\"}\n",TokenMacro.expand(b, listener, "${TEST, arg = \"a \n b  \r\n c\"}\n"));
    }

    @Test
    public void testEscaped() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("${TEST_NESTED}",TokenMacro.expand(b,TaskListener.NULL,"$${TEST_NESTED}"));
        assertEquals("$TEST_NESTED",TokenMacro.expand(b,TaskListener.NULL,"$$TEST_NESTED"));
        assertEquals("$TEST_NESTED{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,TaskListener.NULL,"$$TEST_NESTED$TEST_NESTED"));
        assertEquals("${TEST_NESTED}{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,TaskListener.NULL,"$${TEST_NESTED}$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}$TEST_NESTED",TokenMacro.expand(b,TaskListener.NULL,"$TEST_NESTED$$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}${TEST_NESTED}",TokenMacro.expand(b,TaskListener.NULL,"$TEST_NESTED$${TEST_NESTED}"));        
    }

    @Test
    @Issue("JENKINS-29816")
    public void testEscapedExpandAll() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("${TEST_NESTED}",TokenMacro.expandAll(b, TaskListener.NULL, "$${TEST_NESTED}"));
        assertEquals("$TEST_NESTED",TokenMacro.expandAll(b, TaskListener.NULL, "$$TEST_NESTED"));
        assertEquals("$TEST_NESTED{abc=[def, ghi], jkl=[true]}",TokenMacro.expandAll(b, TaskListener.NULL, "$$TEST_NESTED$TEST_NESTED"));
        assertEquals("${TEST_NESTED}{abc=[def, ghi], jkl=[true]}",TokenMacro.expandAll(b, TaskListener.NULL, "$${TEST_NESTED}$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}$TEST_NESTED",TokenMacro.expandAll(b, TaskListener.NULL, "$TEST_NESTED$$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}${TEST_NESTED}",TokenMacro.expandAll(b, TaskListener.NULL, "$TEST_NESTED$${TEST_NESTED}"));
    }
    
    @Test
    public void testInvalidNoException() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        
        assertEquals("[Error replacing 'ENV' - Undefined parameter Var in token ENV]", TokenMacro.expand(b, TaskListener.NULL, "${ENV, Var=\"HOSTNAME\"}", false, Collections.EMPTY_LIST));        
    }
    
    @Test
    public void testNumeric() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        
        assertEquals("For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!", TokenMacro.expand(b,TaskListener.NULL,"For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!"));
    }

    @Bug(18014)
    @Test
    public void testEscapeCharEscaped() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals("\\{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"\\${TEST_NESTED}"));
        assertEquals("\\{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"\\$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}\\{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"$TEST_NESTED\\$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}\\{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"$TEST_NESTED\\${TEST_NESTED}"));
    }

    @Test
    public void testPrivate() throws Exception {
        List<TokenMacro> privateMacros = new ArrayList<TokenMacro>();
        privateMacros.add(new PrivateTestMacro());
        privateMacros.add(new PrivateTestMacro2());

        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals("TEST_PRIVATE"+j.jenkins.getRootUrl()+"job/foo/1/TEST2_PRIVATE", 
            TokenMacro.expand(b,listener,"${TEST_PRIVATE}${BUILD_URL}${TEST2_PRIVATE}",true,privateMacros));
    }

    @Test
    public void testException() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();

        try {
            TokenMacro.expand(b,listener,"${TEST_NESTEDX}");
            fail();
        } catch(MacroEvaluationException e) {
            // do nothing, just want to catch the exception when it occurs
        }

        assertEquals(" ${TEST_NESTEDX}", TokenMacro.expand(b,listener," ${TEST_NESTEDX}",false,null));
        assertEquals("${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}", TokenMacro.expand(b,listener,"${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}",false,null));
    }

    @Test
    @Issue("JENKINS-38420")
    public void testJENKINS_38420() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("foo");
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
                listener.getLogger().println("<span class=\"timestamp\"><b>14:58:18</b> </span>version: 1.0.0-SNAPSHOT");
                return true;
            }
        });
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        final String result = TokenMacro.expand(b, listener, "${BUILD_LOG_REGEX, regex=\"^.*?version: (.*?)$\", substText=\"$1\", maxMatches=1, showTruncatedLines=false }", false, null);
        assertEquals("1.0.0-SNAPSHOT\n", result);
    }

    @Test
    public void testAutoComplete() throws Exception {
        List<String> suggestions = TokenMacro.getAutoCompleteList("LKJLKJ");
        assertEquals(0, suggestions.size());

        suggestions = TokenMacro.getAutoCompleteList("TES");
        assertEquals(3, suggestions.size());
    }

    @Test
     public void testThatATokenMacroListWithANullEntryDoesNotExplode() throws Exception {
        List<TokenMacro> badList = Lists.newLinkedList();
        badList.add(null);
        badList.add(new PrivateTestMacro());
        badList.add(new PrivateTestMacro2());

        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = StreamTaskListener.fromStdout();
        assertEquals("TEST_PRIVATE" + j.jenkins.getRootUrl() + "job/foo/1/TEST2_PRIVATE",
                TokenMacro.expand(b, listener, "${TEST_PRIVATE}${BUILD_URL}${TEST2_PRIVATE}", true, badList));
    }

    public class PrivateTestMacro extends TokenMacro {
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
        public String evaluate(AbstractBuild<?,?> context, TaskListener listener, String macroName, Map<String,String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return "TEST_PRIVATE";
        }
    }

    public class PrivateTestMacro2 extends TokenMacro {
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
        public String evaluate(AbstractBuild<?,?> context, TaskListener listener, String macroName, Map<String,String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
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
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
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
