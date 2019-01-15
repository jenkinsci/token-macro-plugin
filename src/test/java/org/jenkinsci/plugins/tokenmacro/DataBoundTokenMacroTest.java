/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.tokenmacro;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 *
 * @author acearl
 */
public class DataBoundTokenMacroTest {
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();
    
    @Test
    public void testSimpleDataBoundMacro() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        
        assertEquals("foo", TokenMacro.expand(b, TaskListener.NULL, "${TEST_MACRO, arg=\"foo\"}"));                
    }
    
    @Test
    public void testDataBoundMacroWithFieldAlias() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        
        assertEquals("default = foo, arg2 = -1", TokenMacro.expand(b, TaskListener.NULL, "${ALIAS_MACRO, default=\"foo\"}"));
    }
    
    @Test
    public void testDataBoundMacroWithMethodAlias() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        
        assertEquals("default = foo, arg2 = 4", TokenMacro.expand(b, TaskListener.NULL, "${ALIAS_MACRO, default=\"foo\", int=4}"));
    }


    @Test
    public void testRecursionLimit() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("0 1 2 3 4 5 6 7 8 9 10 $RECURSIVE11", TokenMacro.expand(b, TaskListener.NULL, "$RECURSIVE0"));
    }
    
    @TestExtension
    public static class SimpleDataBoundMacro extends DataBoundTokenMacro {
        
        @Parameter
        public String arg = "default";

        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
            return arg;
        }

        @Override
        public boolean acceptsMacroName(String macroName) {
            return "TEST_MACRO".equals(macroName);
        }        
    }
    
    @TestExtension
    public static class AliasDataBoundMacro extends DataBoundTokenMacro {
        
        @Parameter(required=true, alias="default")
        public String arg = "unknown";
        
        private int arg2 = -1;
        
        @Parameter(alias="int")
        public void setArg2(int arg2) {
            this.arg2 = arg2;
        }
        
        @Override
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
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
        public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
            int level = Integer.parseInt(macroName.substring(9));
            if(level > 10) {
                return "DONE!";
            }
            return level + " $RECURSIVE" + (level+1);
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
