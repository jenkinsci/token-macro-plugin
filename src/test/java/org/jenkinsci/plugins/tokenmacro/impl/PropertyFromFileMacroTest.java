package org.jenkinsci.plugins.tokenmacro.impl;

import org.jenkinsci.plugins.tokenmacro.*;
import com.google.common.collect.ListMultimap;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestExtension;

import java.io.IOException;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class PropertyFromFileMacroTest extends HudsonTestCase {
    private StreamTaskListener listener;

    public void testPropertyFromFileExpansion() throws Exception {
        FreeStyleProject p = createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        BufferedWriter out =
                new BufferedWriter(
                new FileWriter(
                new File(b.getWorkspace().getRemote(),"test.properties")));

        out.write("test.property=success");
        out.close();
        listener = new StreamTaskListener(System.out);
        assertEquals("success",TokenMacro.expand(b, listener, "${PROPFILE,file=\"test.properties\",property=\"test.property\"}"));
    }
}
