
package org.jenkinsci.plugins.tokenmacro.impl;

import com.google.common.io.Files;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
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
    
    /**
     * Charset to be used in order to read logs from the file.
     * @since TODO
     */
    @Parameter(required = false)
    public String charset = null;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return MACRO_NAME.equals(macroName);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        return readLogFile(context.getLogFile());
    }

    public String readLogFile(File file) throws IOException {
        if (regex == null) {
            return "";
        }

        // Prepare patterns and encodings
        String line;
        Pattern pattern = Pattern.compile(regex);
        Charset logCharset = Charset.defaultCharset();
        if (charset != null) {
            try {
                logCharset = Charset.forName(charset);
            } catch (IllegalCharsetNameException ex) {
                throw new IOException("Charset " + charset + " is illegal", ex);
            } catch (UnsupportedCharsetException ex) {
                throw new IOException("Charset " + charset + " is not supported", ex);
            }
        }
                
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), logCharset));
        try {
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    // Match only the top-most line
                    return getTranslatedDescription(matcher);
                }
            }
        } finally {
            reader.close();
        }

        return "";
    }

    private String getTranslatedDescription(Matcher matcher) {
        String result = replacement;
        if (result == null) {
            if (matcher.groupCount() == 0)
                result = "\\0";
            } else {
                result = "\\1";
            }
        }

        // Expand all groups: 1..Count, as well as 0 for the entire pattern
        for (int i = matcher.groupCount(); i >= 0; i--) {
            result = result.replace("\\" + i, matcher.group(i) == null ? "" : matcher.group(i));
        }

        return result;
    }
}
