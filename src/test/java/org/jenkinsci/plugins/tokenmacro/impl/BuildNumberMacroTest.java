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
class BuildNumberMacroTest {

    @Test
    void testBuildNumberMacro(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        String expectedBuildNumber = String.valueOf(b.getNumber());
        assertEquals(expectedBuildNumber, TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${BUILD_NUMBER}"));
    }
}
