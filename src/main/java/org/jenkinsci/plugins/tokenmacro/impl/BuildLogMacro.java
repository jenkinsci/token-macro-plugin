package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.taskdefs.Parallel;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * An EmailContent for build log. Shows last 250 lines of the build log file.
 *
 * @author dvrzalik
 */
@Extension
public class BuildLogMacro extends DataBoundTokenMacro {
    public static final String MACRO_NAME = "BUILD_LOG";

    public static final int MAX_LINES_DEFAULT_VALUE = 250;

    @Parameter
    public int maxLines = MAX_LINES_DEFAULT_VALUE;

    @Parameter
    public int truncTailLines = 0;

    @Parameter
    public boolean escapeHtml = false;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build,null,listener,macroName);
    }

    @Override
    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        if (maxLines <= 0) {
            throw new MacroEvaluationException("Invalid maxLines value: " + maxLines);
        }
        if (truncTailLines < 0) {
            throw new MacroEvaluationException("Invalid truncTailLines value: " + truncTailLines);
        }
        StringBuilder buffer = new StringBuilder();
        try {
            List<String> lines = run.getLog(maxLines);
            // It is OK if this turns out to be a negative value, the entire log will get skipped.
            int nLinesToEval = lines.size() - truncTailLines;
            for (int i = 0; i < nLinesToEval; ++i) {
                String line = lines.get(i);
                if (escapeHtml) {
                    line = StringEscapeUtils.escapeHtml(line);
                }
                buffer.append(line);
                buffer.append('\n');
            }
        } catch (IOException e) {
            listener.getLogger().append("Error getting build log data: " + e.getMessage());
        }

        return buffer.toString();
    }
}
