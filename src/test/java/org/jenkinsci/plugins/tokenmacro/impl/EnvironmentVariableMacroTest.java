package org.jenkinsci.plugins.tokenmacro.impl;

import org.jenkinsci.plugins.tokenmacro.*;
import com.google.common.collect.ListMultimap;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestExtension;

import java.io.IOException;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class EnvironmentVariableMacroTest extends HudsonTestCase {
    private StreamTaskListener listener;

    public void testEnvironmentVariableExpansion() throws Exception {
        FreeStyleProject p = createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        
        listener = new StreamTaskListener(System.out);
        assertEquals("foo",TokenMacro.expand(b, listener,"${ENV,var=\"JOB_NAME\"}"));
    }
}
