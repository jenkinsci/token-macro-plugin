/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.tokenmacro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;

import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 *
 * @author acearl
 */
@WithJenkins
class DataBoundTokenMacroTest {

    @Test
    void testSimpleDataBoundMacro(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("foo", TokenMacro.expand(b, TaskListener.NULL, "${TEST_MACRO, arg=\"foo\"}"));
    }

    @Test
    void testMethodDataBoundMacro(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("DEFAULT", TokenMacro.expand(b, TaskListener.NULL, "${ENUM_MACRO}"));
        assertEquals("DEFAULT", TokenMacro.expand(b, TaskListener.NULL, "${ENUM_MACRO, value=\"DEFAULT\"}"));
        assertEquals("YES", TokenMacro.expand(b, TaskListener.NULL, "${ENUM_MACRO, value=\"YES\"}"));
    }

    @Test
    void testMethodDataBoundMacroThrows(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        assertThrows(MacroEvaluationException.class, () ->
            TokenMacro.expand(b, TaskListener.NULL, "${ENUM_MACRO, value=\"BAD\"}"));
    }

    @Test
    void testDataBoundMacroWithFieldAlias(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals(
                "default = foo, arg2 = -1", TokenMacro.expand(b, TaskListener.NULL, "${ALIAS_MACRO, default=\"foo\"}"));
    }

    @Test
    void testDataBoundMacroWithMethodAlias(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals(
                "default = foo, arg2 = 4",
                TokenMacro.expand(b, TaskListener.NULL, "${ALIAS_MACRO, default=\"foo\", int=4}"));
    }

    @Test
    void testRecursionLimit(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("0 1 2 3 4 5 6 7 8 9 10 DONE!", TokenMacro.expand(b, TaskListener.NULL, "$RECURSIVE0"));
    }

    @TestExtension
    public static class SimpleDataBoundMacro extends DataBoundTokenMacro {

        @Parameter
        public String arg = "default";

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) {
            return arg;
        }

        @Override
        public boolean acceptsMacroName(String macroName) {
            return "TEST_MACRO".equals(macroName);
        }
    }

    public enum Choice {
        DEFAULT,
        YES,
        NO
    }

    @TestExtension
    public static class MethodDataBoundMacro extends DataBoundTokenMacro {

        private Choice value = Choice.DEFAULT;

        @Parameter
        public void setValue(final String value) {
            this.value = Choice.valueOf(value);
        }

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) {
            return this.value.toString();
        }

        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.equals("ENUM_MACRO");
        }
    }

    @TestExtension
    public static class AliasDataBoundMacro extends DataBoundTokenMacro {

        @Parameter(required = true, alias = "default")
        public String arg = "unknown";

        private int arg2 = -1;

        @Parameter(alias = "int")
        public void setArg2(int arg2) {
            this.arg2 = arg2;
        }

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) {
            return String.format("default = %s, arg2 = %d", arg, arg2);
        }

        @Override
        public boolean acceptsMacroName(String macroName) {
            return "ALIAS_MACRO".equals(macroName);
        }
    }

    @TestExtension
    public static class RecursiveDataBoundMacro extends DataBoundTokenMacro {
        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) {
            int level = Integer.parseInt(macroName.substring(9));
            if (level > 10) {
                return "DONE!";
            }
            return level + " $RECURSIVE" + (level + 1);
        }

        @Override
        public boolean acceptsMacroName(String macroName) {
            return macroName.startsWith("RECURSIVE");
        }

        @Override
        public boolean hasNestedContent() {
            return true;
        }
    }
}
