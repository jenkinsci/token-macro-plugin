package org.jenkinsci.plugins.tokenmacro.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.plugins.tokenmacro.Util;

@Extension
public class ChangesSinceLastBuildMacro extends DataBoundTokenMacro {

    public static final String FORMAT_DEFAULT_VALUE = "[%a] %m\\n";
    public static final String PATH_FORMAT_DEFAULT_VALUE = "\\t%p\\n";
    public static final String FORMAT_DEFAULT_VALUE_WITH_PATHS = "[%a] %m%p\\n";
    public static final String DEFAULT_DEFAULT_VALUE = "No changes\n";
    public static final String MACRO_NAME = "CHANGES_SINCE_LAST_BUILD";
    public static final String ALTERNATE_MACRO_NAME = "CHANGES";

    @Parameter
    public boolean showPaths = false;

    @Parameter
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Retain API compatibility.")
    public String format;

    @Parameter
    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Retain API compatibility.")
    public String pathFormat = PATH_FORMAT_DEFAULT_VALUE;

    @Parameter
    public boolean showDependencies = false;

    @Parameter
    public String dateFormat;

    @Parameter
    public String regex;

    @Parameter
    public String replace;

    @Parameter(alias = "default")
    public String def = DEFAULT_DEFAULT_VALUE;

    public ChangesSinceLastBuildMacro() {}

    public ChangesSinceLastBuildMacro(String format, String pathFormat, boolean showPaths) {
        this.format = format;
        this.pathFormat = pathFormat;
        this.showPaths = showPaths;
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME) || macroName.equals(ALTERNATE_MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        List<String> macroNames = new ArrayList<>();
        Collections.addAll(macroNames, MACRO_NAME, ALTERNATE_MACRO_NAME);
        return macroNames;
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(build, null, listener, macroName);
    }

    @Override
    public String evaluate(Run<?, ?> run, FilePath workspace, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {

        if (StringUtils.isEmpty(format)) {
            format = showPaths ? FORMAT_DEFAULT_VALUE_WITH_PATHS : FORMAT_DEFAULT_VALUE;
        }

        DateFormat dateFormatter;
        if (StringUtils.isEmpty(dateFormat)) {
            dateFormatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
        } else {
            dateFormatter = new SimpleDateFormat(dateFormat);
        }

        format = TokenMacro.expandAll(run, workspace, listener, format);

        if (StringUtils.isNotEmpty(pathFormat)) {
            pathFormat = TokenMacro.expandAll(run, workspace, listener, pathFormat);
        }

        StringBuffer buf = new StringBuffer();
        List<ChangeLogSet<?>> changeSets;
        try {
            Method getChangeSets = run.getClass().getMethod("getChangeSets");
            changeSets = (List<ChangeLogSet<?>>) getChangeSets.invoke(run);
        } catch (NoSuchMethodException e) {
            changeSets = Collections.EMPTY_LIST;
        } catch (InvocationTargetException e) {
            changeSets = Collections.EMPTY_LIST;
        } catch (IllegalAccessException e) {
            changeSets = Collections.EMPTY_LIST;
        }

        if (changeSets.size() > 0) {
            for (ChangeLogSet<?> changeSet : changeSets) {
                if (!changeSet.isEmptySet()) {
                    for (ChangeLogSet.Entry entry : changeSet) {
                        Util.printf(buf, format, new ChangesSincePrintfSpec(entry, pathFormat, dateFormatter));
                    }
                } else {
                    buf.append(def);
                }
                if (showDependencies) {
                    Run<?, ?> previousRun = TokenMacro.getPreviousRun(run, listener);
                    if (previousRun instanceof AbstractBuild) {
                        AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) previousRun;
                        for (Entry<AbstractProject, DependencyChange> e : build.getDependencyChanges(
                                        (AbstractBuild) previousRun)
                                .entrySet()) {
                            buf.append("\n=======================\n");
                            buf.append("\nChanges in ")
                                    .append(e.getKey().getName())
                                    .append(":\n");
                            for (AbstractBuild<?, ?> b : e.getValue().getBuilds()) {
                                if (!b.getChangeSet().isEmptySet()) {
                                    for (ChangeLogSet.Entry entry : b.getChangeSet()) {
                                        Util.printf(
                                                buf,
                                                format,
                                                new ChangesSincePrintfSpec(entry, pathFormat, dateFormatter));
                                    }
                                } else {
                                    buf.append(def);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            buf.append(def);
        }

        return buf.toString();
    }

    public class ChangesSincePrintfSpec implements Util.PrintfSpec {

        private final ChangeLogSet.Entry entry;
        private final String pathFormatString;
        private final DateFormat dateFormatter;

        public ChangesSincePrintfSpec(ChangeLogSet.Entry entry, String pathFormatString, DateFormat dateFormatter) {
            this.entry = entry;
            this.pathFormatString = pathFormatString;
            this.dateFormatter = dateFormatter;
        }

        public boolean printSpec(StringBuffer buf, char formatChar) {
            switch (formatChar) {
                case 'a':
                    buf.append(entry.getAuthor().getFullName());
                    return true;
                case 'd': {
                    try {
                        buf.append(dateFormatter.format(new Date(entry.getTimestamp())));
                    } catch (Exception e) {
                        // If it is not implemented or any other problem, swallow the %d
                    }
                    return true;
                }
                case 'm': {
                    String m = entry.getMsg();
                    if (!StringUtils.isEmpty(regex) && !StringUtils.isEmpty(replace)) {
                        m = m.replaceAll(regex, replace);
                    }
                    buf.append(m);
                    if (m == null || !m.endsWith("\n")) {
                        buf.append('\n');
                    }
                    return true;
                }
                case 'p': {
                    try {
                        Collection<? extends AffectedFile> affectedFiles = entry.getAffectedFiles();
                        for (final AffectedFile file : affectedFiles) {
                            Util.printf(buf, pathFormatString, new Util.PrintfSpec() {
                                public boolean printSpec(StringBuffer buf, char formatChar) {
                                    if (formatChar == 'p') {
                                        buf.append(file.getPath());
                                        return true;
                                    } else if (formatChar == 'a') {
                                        buf.append(file.getEditType().getName());
                                        return true;
                                    } else if (formatChar == 'd') {
                                        buf.append(file.getEditType().getDescription());
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            });
                        }
                    } catch (UnsupportedOperationException e) {
                        Collection<String> affectedPaths = entry.getAffectedPaths();
                        for (final String affectedPath : affectedPaths) {
                            Util.printf(buf, pathFormatString, new Util.PrintfSpec() {
                                public boolean printSpec(StringBuffer buf, char formatChar) {
                                    if (formatChar == 'p') {
                                        buf.append(affectedPath);
                                        return true;
                                    } else if (formatChar == 'a') {
                                        buf.append("Unknown");
                                        return true;
                                    } else if (formatChar == 'd') {
                                        buf.append("Unknown");
                                        return true;
                                    } else {
                                        return false;
                                    }
                                }
                            });
                        }
                    }
                    return true;
                }
                case 'r': {
                    try {
                        buf.append(entry.getCommitId());
                    } catch (Exception e) {
                        // If it is not implemented or any other problem, swallow the %r
                    }
                    return true;
                }
                default:
                    return false;
            }
        }
    }
}
