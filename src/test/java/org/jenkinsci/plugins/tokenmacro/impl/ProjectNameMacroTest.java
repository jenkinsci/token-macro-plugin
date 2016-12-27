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
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import static junit.framework.TestCase.assertEquals;

/**
 *
 * @author egutierrez
 */
public class ProjectNameMacroTest {

    private final static String FOLDER_DISPLAY_NAME = "folderdisplay";
    private final static String JOB_DISPLAY_NAME = "jobdisplay";

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void testProjectNameMacro() throws Exception {
        MockFolder folder = j.createFolder("foofolder");
        folder.setDisplayName(FOLDER_DISPLAY_NAME);

        FreeStyleProject p = folder.createProject(FreeStyleProject.class, "foojob");
        p.setDisplayName(JOB_DISPLAY_NAME);
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        String projectDisplayName = TokenMacro.expand(b, StreamTaskListener.fromStdout(),"${PROJECT_DISPLAY_NAME}");
        String projectName = TokenMacro.expand(b, StreamTaskListener.fromStdout(),"${PROJECT_NAME}");

        assertEquals(JOB_DISPLAY_NAME, projectDisplayName);
        assertEquals(String.format("%s » %s", FOLDER_DISPLAY_NAME, JOB_DISPLAY_NAME), projectName);
    }
}
