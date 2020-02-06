package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * An EmailContent for build log lines matching a regular expression. Shows
 * lines matching a regular expression (with optional context lines) from the
 * build log file.
 *
 * @author krwalker@stellarscience.com
 */
@Extension
public class BuildLogRegexMacro extends DataBoundTokenMacro {

    public static final String MACRO_NAME = "BUILD_LOG_REGEX";
    private static final int LINES_BEFORE_DEFAULT_VALUE = 0;
    private static final int LINES_AFTER_DEFAULT_VALUE = 0;
    private static final int MAX_MATCHES_DEFAULT_VALUE = 0;
    private static final int MAX_TAIL_MATCHES_DEFAULT_VALUE = 0;
    @Parameter
    public String regex = "(?i)\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\b";
    @Parameter
    public int linesBefore = LINES_BEFORE_DEFAULT_VALUE;
    @Parameter
    public int linesAfter = LINES_AFTER_DEFAULT_VALUE;
    @Parameter
    public int maxMatches = MAX_MATCHES_DEFAULT_VALUE;
    @Parameter
    public boolean showTruncatedLines = true;
    @Parameter
    public String substText = null; // insert entire line
    @Parameter
    public boolean escapeHtml = false;
    @Parameter
    public String matchedLineHtmlStyle = null;
    @Parameter
    public boolean addNewline = true;
    @Parameter
    public String defaultValue = "";
    @Parameter
    public boolean greedy = true;
    @Parameter
    public int maxTailMatches = MAX_TAIL_MATCHES_DEFAULT_VALUE;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    private boolean startPre(List<String> matchResults, boolean insidePre) {
        if (!insidePre) {
            matchResults.add("<pre>\n");
            insidePre = true;
        }
        return insidePre;
    }

    private boolean stopPre(List<String> matchResults, boolean insidePre) {
        if (insidePre) {
            matchResults.add("</pre>\n");
            insidePre = false;
        }
        return insidePre;
    }

    private void appendContextLine(List<String> matchResults, String line, boolean escapeHtml) {
        if (escapeHtml) {
            line = StringEscapeUtils.escapeHtml(line);
        }
        matchResults.add(line+'\n');
    }

    private void appendMatchedLine(List<String> matchResults, String line, boolean escapeHtml, String style, boolean addNewline) {
        if (escapeHtml) {
            line = StringEscapeUtils.escapeHtml(line);
        }
        StringBuffer buffer = new StringBuffer();
        if (style != null) {
            buffer.append("<b");
            if (style.length() > 0) {
                buffer.append(" style=\"");
                buffer.append(style);
                buffer.append("\"");
            }
            buffer.append(">");
        }
        buffer.append(line);
        if (style != null) {
            buffer.append("</b>");
        }

        if (addNewline) {
            buffer.append('\n');
        }
        matchResults.add(buffer.toString());
    }

    private void appendLinesTruncated(List<String> matchResults, int numLinesTruncated, boolean asHtml) {
        StringBuffer buffer = new StringBuffer();
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
        matchResults.add(buffer.toString());
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build,null,listener,macroName);
    }

    @Override
    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        try {
            BufferedReader reader = new BufferedReader(run.getLogReader());
            String transformedContent = getContent(reader);
            reader.close();
            return transformedContent;
        } catch (IOException ex) {
            listener.error(ex.getMessage());
            return ""; // TODO: Indicate there was an error instead?
        }
    }

    String getContent(BufferedReader reader)
            throws IOException {

        final boolean asHtml = matchedLineHtmlStyle != null;
        escapeHtml = asHtml || escapeHtml;

        final Pattern pattern = Pattern.compile(regex);
        List<String> matchResults = new LinkedList<>();
        int numLinesTruncated = 0;
        int numMatches = 0;
        int numLinesStillNeeded = 0;
        boolean insidePre = false;
        Queue<String> linesBeforeList = new LinkedList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            // Remove console notes (JENKINS-7402)
            line = ConsoleNote.removeNotes(line);

            // Remove any lines before that are no longer needed.
            while (linesBeforeList.size() > linesBefore) {
                linesBeforeList.remove();
                ++numLinesTruncated;
            }
            final Matcher matcher = pattern.matcher(line);
            final StringBuffer sb = new StringBuffer();
            boolean matched = false;
            while (matcher.find()) {
                matched = true;
                if (substText != null) {
                    matcher.appendReplacement(sb, substText);
                } else {
                    break;
                }
            }
            if (matched && (greedy || maxMatches == 0 || (numMatches < maxMatches))) {
                // The current line matches.
                if (showTruncatedLines == true && numLinesTruncated > 0) {
                    // Append information about truncated lines.
                    insidePre = stopPre(matchResults, insidePre);
                    appendLinesTruncated(matchResults, numLinesTruncated, asHtml);
                    numLinesTruncated = 0;
                }
                if (asHtml) {
                    insidePre = startPre(matchResults, insidePre);
                }
                while (!linesBeforeList.isEmpty()) {
                    appendContextLine(matchResults, linesBeforeList.remove(), escapeHtml);
                }
                // Append the (possibly transformed) current line.
                if (substText != null) {
                    matcher.appendTail(sb);
                    line = sb.toString();
                }
                appendMatchedLine(matchResults, line, escapeHtml, matchedLineHtmlStyle, addNewline);
                ++numMatches;
                // Set up to add numLinesStillNeeded
                numLinesStillNeeded = linesAfter;
            } else {
                // The current line did not match.
                if (numLinesStillNeeded > 0) {
                    // Append this line as a line after.
                    appendContextLine(matchResults, line, escapeHtml);
                    --numLinesStillNeeded;
                } else {
                    // Store this line as a possible line before.
                    linesBeforeList.offer(line);
                }
            }
            if (maxMatches != 0 && numMatches >= maxMatches && numLinesStillNeeded == 0) {
                break;
            }
        }
        if (showTruncatedLines == true) {
            // Count the rest of the lines.
            // Include any lines in linesBefore.
            while (linesBeforeList.size() > 0) {
                linesBeforeList.remove();
                ++numLinesTruncated;
            }
            if (line != null) {
                // Include the rest of the lines that haven't been read in.
                while ((line = reader.readLine()) != null) {
                    ++numLinesTruncated;
                }
            }
            if (numLinesTruncated > 0) {
                insidePre = stopPre(matchResults, insidePre);
                appendLinesTruncated(matchResults, numLinesTruncated, asHtml);
            }
        }
        insidePre = stopPre(matchResults, insidePre);
        if (matchResults.size() == 0) {
            return defaultValue;
        }
        if (maxTailMatches > 0 && matchResults.size() > maxTailMatches) {
            matchResults = matchResults.subList(matchResults.size() - maxTailMatches, matchResults.size());
        }
        return String.join("", matchResults);
    }
}
