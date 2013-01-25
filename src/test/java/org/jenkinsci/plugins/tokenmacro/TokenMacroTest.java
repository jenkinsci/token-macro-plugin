package org.jenkinsci.plugins.tokenmacro;

import com.google.common.collect.ListMultimap;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class TokenMacroTest extends HudsonTestCase {
    private StreamTaskListener listener;

    public void testBasics() throws Exception {
        FreeStyleProject p = createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = new StreamTaskListener(System.out);
        assertEquals(hudson.getRootUrl()+"job/foo/1/",TokenMacro.expand(b, listener,"${BUILD_URL}"));
        assertEquals(hudson.getRootUrl()+"job/foo/1/",TokenMacro.expand(b, listener,"$BUILD_URL"));

        assertEquals("{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"${TEST,abc=\"def\",abc=\"ghi\",jkl=true}"));
    }

    public void testNested() throws Exception {
        FreeStyleProject p = createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = new StreamTaskListener(System.out);

        assertEquals("{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"${TEST_NESTED}"));
    }
    
    public void testEscaped() throws Exception {
        FreeStyleProject p = createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = new StreamTaskListener(System.out);
        assertEquals("\\${TEST_NESTED}",TokenMacro.expand(b,listener,"\\${TEST_NESTED}"));
        assertEquals("\\$TEST_NESTED",TokenMacro.expand(b,listener,"\\$TEST_NESTED"));
        assertEquals("\\$TEST_NESTED{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"\\$TEST_NESTED$TEST_NESTED"));
        assertEquals("\\${TEST_NESTED}{abc=[def, ghi], jkl=[true]}",TokenMacro.expand(b,listener,"\\${TEST_NESTED}$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}\\$TEST_NESTED",TokenMacro.expand(b,listener,"$TEST_NESTED\\$TEST_NESTED"));
        assertEquals("{abc=[def, ghi], jkl=[true]}\\${TEST_NESTED}",TokenMacro.expand(b,listener,"$TEST_NESTED\\${TEST_NESTED}"));
    }

    public void testPrivate() throws Exception {
        List<TokenMacro> privateMacros = new ArrayList<TokenMacro>();
        privateMacros.add(new PrivateTestMacro());
        privateMacros.add(new PrivateTestMacro2());

        FreeStyleProject p = createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = new StreamTaskListener(System.out);
        assertEquals("TEST_PRIVATE"+hudson.getRootUrl()+"job/foo/1/TEST2_PRIVATE", 
            TokenMacro.expand(b,listener,"${TEST_PRIVATE}${BUILD_URL}${TEST2_PRIVATE}",true,privateMacros));
    }

    public void testException() throws Exception {
        boolean threwException = false;
        FreeStyleProject p = createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        listener = new StreamTaskListener(System.out);

        try {
            String s = TokenMacro.expand(b,listener,"${TEST_NESTEDX}");
            fail();
        } catch(MacroEvaluationException e) {
            // do nothing, just want to catch the exception when it occurs
        }

        assertEquals("${TEST_NESTEDX}", TokenMacro.expand(b,listener,"${TEST_NESTEDX}",false,null));
        assertEquals("${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}",
            TokenMacro.expand(b,listener,"${TEST_NESTEDX,abc=\"def\",abc=\"ghi\",jkl=true}",false,null));
    }

    public class PrivateTestMacro extends TokenMacro {
        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals("TEST_PRIVATE");
        }

        @Override
        public String evaluate(AbstractBuild<?,?> context, TaskListener listener, String macroName, Map<String,String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return "TEST_PRIVATE";
        }
    }

    public class PrivateTestMacro2 extends TokenMacro {
        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals("TEST2_PRIVATE");
        }

        @Override
        public String evaluate(AbstractBuild<?,?> context, TaskListener listener, String macroName, Map<String,String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return "TEST2_PRIVATE";
        }
    }

    @TestExtension
    public static class TestMacro extends TokenMacro {
        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals("TEST");
        }

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return argumentMultimap.toString();
        }
    }

    @TestExtension
    public static class NestedTestMacro extends TokenMacro {
        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals("TEST_NESTED");
        }

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName, Map<String, String> arguments, ListMultimap<String, String> argumentMultimap) throws MacroEvaluationException, IOException, InterruptedException {
            return "${TEST,abc=\"def\",abc=\"ghi\",jkl=true}";
        }

        @Override
        public boolean hasNestedContent() {
            return true;
        }
    }
}
