/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.StreamTaskListener;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author acearl
 */
@WithJenkins
class AdminEmailMacroTest {

    @Test
    void testAdminAddressMacro(JenkinsRule j) throws Exception {
        JenkinsLocationConfiguration.get().setAdminAddress("mickey@disney.com");

        FreeStyleProject p = j.createFreeStyleProject("foo");
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        assertEquals("mickey@disney.com", TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${ADMIN_EMAIL}"));
    }
}
