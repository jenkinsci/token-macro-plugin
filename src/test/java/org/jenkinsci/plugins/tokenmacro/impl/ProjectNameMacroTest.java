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
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author egutierrez
 */
@WithJenkins
class ProjectNameMacroTest {

    private static final String FOLDER_DISPLAY_NAME = "folderdisplay";
    private static final String JOB_DISPLAY_NAME = "jobdisplay";

    @Test
    void testProjectNameMacro(JenkinsRule j) throws Exception {
        MockFolder folder = j.createFolder("foofolder");
        folder.setDisplayName(FOLDER_DISPLAY_NAME);

        FreeStyleProject p = folder.createProject(FreeStyleProject.class, "foojob");
        p.setDisplayName(JOB_DISPLAY_NAME);
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        String projectDisplayName = TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${PROJECT_DISPLAY_NAME}");
        String projectName = TokenMacro.expand(b, StreamTaskListener.fromStdout(), "${PROJECT_NAME}");

        assertEquals(JOB_DISPLAY_NAME, projectDisplayName);
        assertEquals(String.format("%s » %s", FOLDER_DISPLAY_NAME, JOB_DISPLAY_NAME), projectName);
    }
}
