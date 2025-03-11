/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.StreamTaskListener;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author egutierrez
 */
@WithJenkins
class ProjectUrlMacroTest {

    private static final String JOB_NAME = "foo";

    @Test
    void testProjectUrlMacro(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject(JOB_NAME);
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        String jenkinsUrl = j.getURL().toExternalForm();
        String expectedProjectUrl = String.format("%sjob/%s/", jenkinsUrl, JOB_NAME);
        assertEquals(expectedProjectUrl, TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${PROJECT_URL}"));
    }
}
