package org.jenkinsci.plugins.tokenmacro.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.model.*;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.plugins.tokenmacro.Util;

public abstract class AbstractChangesSinceMacro extends DataBoundTokenMacro {

    @Parameter
    public boolean reverse = false;

    @Parameter
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Retain API compatibility.")
    public String format;

    @Parameter
    public boolean showPaths = false;

    @Parameter
    public String changesFormat;

    @Parameter
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Retain API compatibility.")
    public String pathFormat = "\\t%p\\n";

    @Parameter
    public boolean showDependencies = false;

    @Parameter
    public String dateFormat;

    @Parameter
    public String regex;

    @Parameter
    public String replace;

    @Parameter(alias = "default")
    public String def = ChangesSinceLastBuildMacro.DEFAULT_DEFAULT_VALUE;

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, null, listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        // No previous build so bail
        if (TokenMacro.getPreviousRun(run, listener) == null) {
            return "";
        }

        if (StringUtils.isEmpty(format)) {
            format = getDefaultFormatValue();
        }

        format = TokenMacro.expandAll(run, workspace, listener, format);

        if (StringUtils.isNotEmpty(pathFormat)) {
            pathFormat = TokenMacro.expandAll(run, workspace, listener, pathFormat);
        }

        StringBuffer sb = new StringBuffer();
        final Run startRun;
        final Run endRun;
        if (reverse) {
            startRun = run;
            endRun = getFirstIncludedRun(run, listener);
        } else {
            startRun = getFirstIncludedRun(run, listener);
            endRun = run;
        }
        Run<?, ?> currentBuild = null;
        while (currentBuild != endRun) {
            if (currentBuild == null) {
                currentBuild = startRun;
            } else {
                if (reverse) {
                    currentBuild = currentBuild.getPreviousBuild();
                } else {
                    currentBuild = currentBuild.getNextBuild();
                }
            }
            appendBuild(sb, listener, currentBuild);
        }

        return sb.toString();
    }

    private void appendBuild(StringBuffer buf, final TaskListener listener, final Run<?, ?> currentRun)
            throws MacroEvaluationException {
        // Use this object since it already formats the changes per build
        final ChangesSinceLastBuildMacro changes = new ChangesSinceLastBuildMacro(changesFormat, pathFormat, showPaths);
        changes.showDependencies = showDependencies;
        changes.dateFormat = dateFormat;
        changes.regex = regex;
        changes.replace = replace;
        changes.def = def;

        Util.printf(buf, format, new Util.PrintfSpec() {
            public boolean printSpec(StringBuffer buf, char formatChar) {
                switch (formatChar) {
                    case 'c':
                        try {
                            buf.append(changes.evaluate(
                                    currentRun, null, listener, ChangesSinceLastBuildMacro.MACRO_NAME));
                        } catch (MacroEvaluationException e) {
                            // ignore this
                        } catch (IOException e) {
                            // ignore this
                        } catch (InterruptedException e) {
                            // ignore this
                        }
                        return true;
                    case 'n':
                        buf.append(currentRun.getNumber());
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    public abstract String getDefaultFormatValue();

    public abstract String getShortHelpDescription();

    public abstract Run<?, ?> getFirstIncludedRun(Run<?, ?> build, TaskListener listener);
}
