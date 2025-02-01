/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.tokenmacro.impl;

import static junit.framework.TestCase.assertEquals;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 *
 * @author egutierrez
 */
public class JenkinsUrlMacroTest {

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testJenkinsUrlMacro() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        String expectedJenkinsUrl = j.getURL().toExternalForm();
        assertEquals(expectedJenkinsUrl, TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${JENKINS_URL}"));
        assertEquals(expectedJenkinsUrl, TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${HUDSON_URL}"));
    }
}
