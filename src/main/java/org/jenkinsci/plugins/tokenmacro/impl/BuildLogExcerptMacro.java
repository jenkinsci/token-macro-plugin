package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * @author <a href="mailto:nicolas.deloof@cloudbees.com">Nicolas De loof</a>
 */
@Extension
public class BuildLogExcerptMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_LOG_EXCERPT";

    @Parameter(required=true)
    public String start;

    @Parameter(required=true)
    public String end;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context,null,listener,macroName);
    }

    @Override
    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        try {
            BufferedReader reader = new BufferedReader(run.getLogReader());
            try {
                return getContent(reader);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        } catch (IOException e) {
            listener.getLogger().println("Error getting BUILD_LOG_EXCERPT - " + e.getMessage());
            return ""; // TODO: Indicate there was an error instead?
        }
    }

    String getContent(BufferedReader reader) throws IOException {
        Pattern startPattern = Pattern.compile(start);
        Pattern endPattern = Pattern.compile(end);

        StringBuilder buffer = new StringBuilder();
        String line;
        boolean started = false;
        while ((line = reader.readLine()) != null) {
            line = ConsoleNote.removeNotes(line);

            if (startPattern.matcher(line).matches()) {
                started = true;
                continue;
            }
            if (endPattern.matcher(line).matches()) break;

            if (started) buffer.append(line).append('\n');
        }
        return buffer.toString();
    }
}
