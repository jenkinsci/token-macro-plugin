package org.jenkinsci.plugins.tokenmacro.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ArrayListMultimap;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.EditType;
import hudson.util.StreamTaskListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangesSinceLastBuildMacroTest {

    private ChangesSinceLastBuildMacro changesSinceLastBuildMacro;
    private TaskListener listener;

    @BeforeEach
    void setup() {
        changesSinceLastBuildMacro = new ChangesSinceLastBuildMacro();
        listener = StreamTaskListener.fromStdout();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Phoenix"));
    }

    @Test
    void testShouldGetChangesForLatestBuild() throws Exception {
        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals("[Ash Lux] Changes for a successful build.\n\n", content);
    }

    @Test
    void testShouldGetChangesForLatestBuildEvenWhenPreviousBuildsExist() throws Exception {
        AbstractBuild failureBuild = createBuild2(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals("[Ash Lux] Changes for a successful build.\n\n", content);
    }

    @Test
    void testShouldPrintDate() throws Exception {
        changesSinceLastBuildMacro.format = "%d";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        // Java 21 changed the SHORT date format... https://bugs.openjdk.org/browse/JDK-8225245
        assertThat(content, matchesPattern("Oct 21, 2013, 7:39:00\\hPM"));
    }

    @Test
    void testShouldPrintRevision() throws Exception {
        changesSinceLastBuildMacro.format = "%r";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals("REVISION", content);
    }

    @Test
    void testShouldPrintPath() throws Exception {
        changesSinceLastBuildMacro.format = "%p";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals("\tPATH1\n\tPATH2\n\tPATH3\n", content);
    }

    @Test
    void testWhenShowPathsIsTrueShouldPrintPath() throws Exception {
        changesSinceLastBuildMacro.showPaths = true;

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals(
		        """
                        [Ash Lux] Changes for a successful build.
                        \tPATH1
                        \tPATH2
                        \tPATH3
				        
                        """,
                content);
    }

    @Test
    void testDateFormatString() throws Exception {
        changesSinceLastBuildMacro.format = "%d";
        changesSinceLastBuildMacro.dateFormat = "MMM d, yyyy HH:mm:ss";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals("Oct 21, 2013 19:39:00", content);
    }

    @Test
    void testTypeFormatStringWithNoGetAffectedFiles() throws Exception {
        changesSinceLastBuildMacro.showPaths = true;
        changesSinceLastBuildMacro.pathFormat = "\t%p\t%a\n";

        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals(
		        """
                        [Ash Lux] Changes for a successful build.
                        \tPATH1\tUnknown
                        \tPATH2\tUnknown
                        \tPATH3\tUnknown
				        
                        """,
                content);
    }

    @Test
    void testTypeFormatStringWithAffectedFiles() throws Exception {
        changesSinceLastBuildMacro.showPaths = true;
        changesSinceLastBuildMacro.pathFormat = "\t%p\t%a - %d\n";

        AbstractBuild currentBuild =
                createBuildWithAffectedFiles(Result.SUCCESS, 42, "Changes for a successful build.");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals(
		        """
                        [Ash Lux] Changes for a successful build.
                        \tPATH1\tadd - The file was added
                        \tPATH2\tdelete - The file was removed
                        \tPATH3\tedit - The file was modified
				        
                        """,
                content);
    }

    @Test
    void testRegexReplace() throws Exception {
        changesSinceLastBuildMacro.regex = "<defectId>(DEFECT-[0-9]+)</defectId><message>(.*)</message>";
        changesSinceLastBuildMacro.replace = "[$1] $2";
        changesSinceLastBuildMacro.format = "%m\\n";

        AbstractBuild currentBuild = createBuildWithAffectedFiles(
                Result.SUCCESS, 42, "<defectId>DEFECT-666</defectId><message>Initial commit</message>");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals("[DEFECT-666] Initial commit\n\n", content);
    }

    @Test
    void testShouldPrintDefaultMessageWhenNoChanges() throws Exception {
        AbstractBuild currentBuild = createBuildWithNoChanges(Result.SUCCESS, 42);

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals(ChangesSinceLastBuildMacro.DEFAULT_DEFAULT_VALUE, content);
    }

    @Test
    void testShouldPrintMessageWhenNoChanges() throws Exception {
        changesSinceLastBuildMacro.def = "another default message\n";
        AbstractBuild currentBuild = createBuildWithNoChanges(Result.SUCCESS, 42);

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals("another default message\n", content);
    }

    @Test
    void testShouldDefaultToNotEscapeHtml() throws Exception {
        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "<b>bold</b>");

        String content =
                changesSinceLastBuildMacro.evaluate(currentBuild, listener, ChangesSinceLastBuildMacro.MACRO_NAME);

        assertEquals("[Ash Lux] <b>bold</b>\n\n", content);
    }

    @Test
    void testShouldEscapeHtmlWhenArgumentEscapeHtmlSetToTrue() throws Exception {
        AbstractBuild currentBuild = createBuild(Result.SUCCESS, 42, "<b>bold</b>");

        final Map<String, String> arguments = Collections.singletonMap("escapeHtml", "true");
        final ArrayListMultimap<String, String> listMultimap = ArrayListMultimap.create();
        listMultimap.put("escapeHtml", "true");
        String content = changesSinceLastBuildMacro.evaluate(
                currentBuild, null, listener, ChangesSinceLastBuildMacro.MACRO_NAME, arguments, listMultimap);

        assertEquals("[Ash Lux] &lt;b&gt;bold&lt;/b&gt;\n\n", content);
    }

    private AbstractBuild createBuild(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet changes1 = createChangeLog(message);
        when(build.getChangeSets()).thenReturn(Collections.singletonList(changes1));

        return build;
    }

    private AbstractBuild createBuildWithAffectedFiles(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet changes1 = createChangeLogWithAffectedFiles(message);
        when(build.getChangeSets()).thenReturn(Collections.singletonList(changes1));

        return build;
    }

    private ChangeLogSet createChangeLog(String message) {
        ChangeLogSet changes = mock(ChangeLogSet.class);

        List<ChangeLogSet.Entry> entries = new LinkedList<>();
        ChangeLogSet.Entry entry = new ChangeLogEntry(message, "Ash Lux");
        entries.add(entry);
        when(changes.iterator()).thenReturn(entries.iterator());

        return changes;
    }

    private AbstractBuild createBuildWithNoChanges(Result result, int buildNumber) {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet changes1 = createEmptyChangeLog();
        when(build.getChangeSets()).thenReturn(Collections.singletonList(changes1));

        return build;
    }

    private ChangeLogSet createEmptyChangeLog() {
        ChangeLogSet changes = mock(ChangeLogSet.class);
        List<ChangeLogSet.Entry> entries = Collections.emptyList();
        when(changes.isEmptySet()).thenReturn(true);

        return changes;
    }

    private ChangeLogSet createChangeLogWithAffectedFiles(String message) {
        ChangeLogSet changes = mock(ChangeLogSet.class);

        List<ChangeLogSet.Entry> entries = new LinkedList<>();
        ChangeLogSet.Entry entry = new ChangeLogEntryWithAffectedFiles(message, "Ash Lux");
        entries.add(entry);
        when(changes.iterator()).thenReturn(entries.iterator());

        return changes;
    }

    private static class ChangeLogEntry extends ChangeLogSet.Entry {

        final String message;
        final String author;

        public ChangeLogEntry(String message, String author) {
            this.message = message;
            this.author = author;
        }

        @Override
        public String getMsg() {
            return message;
        }

        @Override
        public User getAuthor() {
            User user = mock(User.class);
            when(user.getFullName()).thenReturn(author);
            return user;
        }

        @Override
        public Collection<String> getAffectedPaths() {
            return new ArrayList<>() {
	            {
		            add("PATH1");
		            add("PATH2");
		            add("PATH3");
	            }
            };
        }

        @Override
        public String getCommitId() {
            return "REVISION";
        }

        @Override
        public long getTimestamp() {
            // 10/21/13 7:39 PM
            return 1382409540000L;
        }
    }

    private static class TestAffectedFile implements AffectedFile {

        private final String path;
        private final EditType type;

        public TestAffectedFile(String path, EditType type) {
            this.path = path;
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public EditType getEditType() {
            return type;
        }
    }

    private static class ChangeLogEntryWithAffectedFiles extends ChangeLogEntry {

        public ChangeLogEntryWithAffectedFiles(String message, String author) {
            super(message, author);
        }

        @Override
        public Collection<AffectedFile> getAffectedFiles() {
            return new ArrayList<>() {
	            {
		            add(new TestAffectedFile("PATH1", EditType.ADD));
		            add(new TestAffectedFile("PATH2", EditType.DELETE));
		            add(new TestAffectedFile("PATH3", EditType.EDIT));
	            }
            };
        }
    }

    private AbstractBuild createBuild2(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet changes1 = createChangeLog2(message);
        return build;
    }

    public ChangeLogSet createChangeLog2(String message) {
        ChangeLogSet changes = mock(ChangeLogSet.class);
        List<ChangeLogSet.Entry> entries = new LinkedList<>();
        ChangeLogSet.Entry entry = new ChangeLogEntry(message, "Ash Lux");
        entries.add(entry);
        return changes;
    }
}
