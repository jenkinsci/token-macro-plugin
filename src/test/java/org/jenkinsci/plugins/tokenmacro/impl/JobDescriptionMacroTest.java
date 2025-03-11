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
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author acearl
 */
@WithJenkins
class JobDescriptionMacroTest {

    @Test
    void testNoDescription(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("noDescription");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${JOB_DESCRIPTION}"));
    }

    @Test
    void testSimple(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("testSimple");
        p.setDescription("This is the description");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("This is the description", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${JOB_DESCRIPTION}"));
    }

    @Issue("JENKINS-32012")
    @Test
    void testRemoveNewlines(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("testRemoveNewlines");
        p.setDescription("This is a description\nwith a newline");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("This is a description with a newline", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${JOB_DESCRIPTION, removeNewlines=true}"));
    }
}
