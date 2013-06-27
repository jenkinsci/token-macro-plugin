
package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Uses a regular expression to find a single log entry and generates
 * a new output using the capture groups from it. This is partially
 * based on the description-setter plugin
 * (https://github.com/jenkinsci/description-setter-plugin).
 *
 */
@Extension
public final class LogRegExMacro extends DataBoundTokenMacro {
    private static final String MACRO_NAME = "LOG_REGEX";

    @Parameter(required=true)
    public String regex = null;

    @Parameter(required=true)
    public String replacement = null;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        return readLogFile(context.getLogFile());
    }

    public String readLogFile(File file) throws IOException
    {
        if (regex == null) {
            return "";
        }

        // Assume default encoding and text files
        String line;
        Pattern pattern = Pattern.compile(regex);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while ((line = reader.readLine()) != null)
        {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find())
            {
                // Match only the top-most line
                return getTranslatedDescription(matcher);
            }
        }

        return "";
    }

    private String getTranslatedDescription(Matcher matcher)
    {
        String result = replacement;
        if (result == null) {
            if (matcher.groupCount() == 0)
            {
                result = "\\0";
            }
            else
            {
                result = "\\1";
            }
        }

        // Expand all groups: 1..Count, as well as 0 for the entire pattern
        for (int i = matcher.groupCount(); i >= 0; i--)
        {
            result = result.replace(
                "\\" + i,
                matcher.group(i) == null ? "" : matcher.group(i));
        }

        return result;
    }
}
