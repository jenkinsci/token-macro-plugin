package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.model.*;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.plugins.tokenmacro.Util;

abstract public class AbstractChangesSinceMacro
        extends DataBoundTokenMacro {

    @Parameter
    public boolean reverse = false;
    @Parameter
    public String format;
    @Parameter
    public boolean showPaths = false;
    @Parameter
    public String changesFormat;
    @Parameter
    public String pathFormat = "\\t%p\\n";
    @Parameter
    public boolean showDependencies = false;
    @Parameter
    public String dateFormat;
    @Parameter
    public String regex;
    @Parameter
    public String replace;
    @Parameter(alias="default")
    public String def = ChangesSinceLastBuildMacro.DEFAULT_DEFAULT_VALUE;

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        // No previous build so bail
        if (TokenMacro.getPreviousRun(build, listener) == null) {
            return "";
        }

        if (StringUtils.isEmpty(format)) {
            format = getDefaultFormatValue();
        }

        StringBuffer sb = new StringBuffer();
        final Run startBuild;
        final Run endBuild;
        if (reverse) {
            startBuild = build;
            endBuild = getFirstIncludedRun(build, listener);
        } else {
            startBuild = getFirstIncludedRun(build, listener);
            endBuild = build;
        }
        Run<?, ?> currentBuild = null;
        while (currentBuild != endBuild) {
            if (currentBuild == null) {
                currentBuild = startBuild;
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

    private void appendBuild(StringBuffer buf,
                             final TaskListener listener,
                             final Run<?, ?> currentRun)
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
                        if(currentRun instanceof AbstractBuild) {
                            try {
                                buf.append(changes.evaluate((AbstractBuild) currentRun, listener, ChangesSinceLastBuildMacro.MACRO_NAME));
                            } catch (MacroEvaluationException e) {
                                // ignore this
                            } catch (IOException e) {
                                // ignore this
                            } catch (InterruptedException e) {
                                // ignore this
                            }
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

    @Override
    public boolean hasNestedContent() {
        return true;
    }

    public abstract String getDefaultFormatValue();

    public abstract String getShortHelpDescription();

    public abstract Run<?,?> getFirstIncludedRun(Run<?,?> build, TaskListener listener);
}

