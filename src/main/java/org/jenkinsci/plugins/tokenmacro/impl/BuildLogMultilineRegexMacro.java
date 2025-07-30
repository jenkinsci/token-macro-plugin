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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * An EmailContent for build log segments matching a regular expression. The
 * regular expression will be matched against the whole content of the build
 * log, including line terminators. Shows build log segments matching a regular
 * expression from the build log file.
 *
 * @author krwalker@stellarscience.com
 */
@Extension
public class BuildLogMultilineRegexMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_LOG_MULTILINE_REGEX";
    public static final int MAX_MATCHES_DEFAULT_VALUE = 0;

    @Parameter(required = true)
    public String regex;

    @Parameter
    public int maxMatches = MAX_MATCHES_DEFAULT_VALUE;

    @Parameter
    public boolean showTruncatedLines = true;

    @Parameter
    public String substText = null; // insert entire segment

    @Parameter
    public String matchedSegmentHtmlStyle = null;

    private static final Pattern LINE_TERMINATOR_PATTERN = Pattern.compile("(?<=.)\\r?\\n");

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    private boolean startPre(StringBuilder buffer, boolean insidePre) {
        if (!insidePre) {
            buffer.append("<pre>\n");
            insidePre = true;
        }
        return insidePre;
    }

    private boolean stopPre(StringBuilder buffer, boolean insidePre) {
        if (insidePre) {
            buffer.append("</pre>\n");
            insidePre = false;
        }
        return insidePre;
    }

    private void appendMatchedSegment(StringBuilder buffer, String segment, boolean escapeHtml, String style) {
        if (escapeHtml) {
            segment = StringEscapeUtils.escapeHtml4(segment);
        }
        if (style != null) {
            buffer.append("<b");
            if (style.length() > 0) {
                buffer.append(" style=\"");
                buffer.append(style);
                buffer.append("\"");
            }
            buffer.append(">");
        }
        buffer.append(segment);
        if (style != null) {
            buffer.append("</b>");
        }
        buffer.append('\n');
    }

    private void appendLinesTruncated(StringBuilder buffer, int numLinesTruncated, boolean asHtml) {
        // This format comes from hudson.model.Run.getLog(maxLines).
        if (asHtml) {
            buffer.append("<p>");
        }
        buffer.append("[...truncated ");
        buffer.append(numLinesTruncated);
        buffer.append(" lines...]");
        if (asHtml) {
            buffer.append("</p>");
        }
        buffer.append('\n');
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, null, listener, macroName);
    }

    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        try {
            BufferedReader reader = new BufferedReader(run.getLogReader());
            try {
                return getContent(reader);
            } finally {
                reader.close();
            }
        } catch (IOException ex) {
            listener.error(ex.getMessage());
            return ""; // TODO: Indicate there was an error instead?
        }
    }

    private String getContent(BufferedReader reader) throws IOException {
        final Pattern pattern = Pattern.compile(regex);
        final boolean asHtml = matchedSegmentHtmlStyle != null;
        escapeHtml = asHtml || escapeHtml;

        StringBuilder line = new StringBuilder();
        StringBuilder fullLog = new StringBuilder();
        int ch;
        // Buffer log contents including line terminators, and remove console notes
        while ((ch = reader.read()) != -1) {
            if (ch == '\r' || ch == '\n') {
                if (line.length() > 0) {
                    // Remove console notes (JENKINS-7402)
                    fullLog.append(ConsoleNote.removeNotes(line.toString()));
                    line.setLength(0);
                }
                fullLog.append((char) ch);
            } else {
                line.append((char) ch);
            }
        }
        // Buffer the final log line if it has no line terminator
        if (line.length() > 0) {
            // Remove console notes (JENKINS-7402)
            fullLog.append(ConsoleNote.removeNotes(line.toString()));
        }
        StringBuilder content = new StringBuilder();
        int numMatches = 0;
        boolean insidePre = false;
        int lastMatchEnd = 0;
        final Matcher matcher = pattern.matcher(fullLog);
        while (matcher.find()) {
            if (maxMatches != 0 && ++numMatches > maxMatches) {
                break;
            }
            if (showTruncatedLines) {
                if (matcher.start() > lastMatchEnd) {
                    // Append information about truncated lines.
                    int numLinesTruncated = countLineTerminators(fullLog.subSequence(lastMatchEnd, matcher.start()));
                    if (numLinesTruncated > 0) {
                        insidePre = stopPre(content, insidePre);
                        appendLinesTruncated(content, numLinesTruncated, asHtml);
                    }
                }
            }
            if (asHtml) {
                insidePre = startPre(content, insidePre);
            }
            if (substText != null) {
                final StringBuffer substBuf = new StringBuffer();
                matcher.appendReplacement(substBuf, substText);
                // Remove prepended text between matches
                final String segment = substBuf.substring(matcher.start() - lastMatchEnd);
                appendMatchedSegment(content, segment, escapeHtml, matchedSegmentHtmlStyle);
            } else {
                appendMatchedSegment(content, matcher.group(), escapeHtml, matchedSegmentHtmlStyle);
            }
            lastMatchEnd = matcher.end();
        }
        if (showTruncatedLines) {
            if (fullLog.length() > lastMatchEnd) {
                // Append information about truncated lines.
                int numLinesTruncated = countLineTerminators(fullLog.subSequence(lastMatchEnd, fullLog.length()));
                if (numLinesTruncated > 0) {
                    insidePre = stopPre(content, insidePre);
                    appendLinesTruncated(content, numLinesTruncated, asHtml);
                }
            }
        }
        stopPre(content, insidePre);
        return content.toString();
    }

    private int countLineTerminators(CharSequence charSequence) {
        int lineTerminatorCount = 0;
        Matcher matcher = LINE_TERMINATOR_PATTERN.matcher(charSequence);
        while (matcher.find()) {
            ++lineTerminatorCount;
        }
        return lineTerminatorCount;
    }

    @Override
    public boolean handlesHtmlEscapeInternally() {
        return true;
    }
}
