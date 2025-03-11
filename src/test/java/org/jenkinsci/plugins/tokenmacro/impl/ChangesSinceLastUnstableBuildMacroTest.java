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

class ChangesSinceLastUnstableBuildMacroTest {

    private ChangesSinceLastUnstableBuildMacro content;
    private TaskListener listener;

    @BeforeEach
    void setUp() {
        content = new ChangesSinceLastUnstableBuildMacro();
        listener = StreamTaskListener.fromStdout();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Phoenix"));
    }

    @Test
    void testGetContent_shouldGetNoContentSinceUnstableBuildIfNoPreviousBuild() throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);

        String contentStr = content.evaluate(build, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals("", contentStr);
    }

    @Test
    void testGetContent_shouldGetPreviousBuildFailures() throws Exception {
        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild4(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals(
		        """
                        Changes for Build #41
                        [Ash Lux] Changes for a failed build.
				        
				        
                        Changes for Build #42
                        [Ash Lux] Changes for a successful build.
				        
				        
                        """,
                contentStr);
    }

    @Test
    void testGetContent_whenReverseOrderIsTrueShouldReverseOrderOfChanges() throws Exception {
        content.reverse = true;

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild4(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals(
		        """
                        Changes for Build #42
                        [Ash Lux] Changes for a successful build.
				        
				        
                        Changes for Build #41
                        [Ash Lux] Changes for a failed build.
				        
				        
                        """,
                contentStr);
    }

    @Test
    void testGetContent_shouldGetPreviousBuildsThatArentUnstable_HUDSON3519() throws Exception {
        // Test for HUDSON-3519

        AbstractBuild successfulBuild = createBuild3(Result.SUCCESS, 2, "Changes for a successful build.");

        AbstractBuild unstableBuild = createBuild2(Result.UNSTABLE, 3, "Changes for an unstable build.");

        AbstractBuild abortedBuild = createBuild(Result.ABORTED, 4, "Changes for an aborted build.");
        when(abortedBuild.getPreviousBuild()).thenReturn(unstableBuild);

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 5, "Changes for a failed build.");
        when(failureBuild.getPreviousBuild()).thenReturn(abortedBuild);
        when(abortedBuild.getNextBuild()).thenReturn(failureBuild);

        AbstractBuild notBuiltBuild = createBuild(Result.NOT_BUILT, 6, "Changes for a not-built build.");
        when(notBuiltBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(notBuiltBuild);

        AbstractBuild currentBuild = createBuild4(Result.UNSTABLE, 7, "Changes for an unstable build.");
        when(currentBuild.getPreviousBuild()).thenReturn(notBuiltBuild);
        when(notBuiltBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals(
		        """
                        Changes for Build #4
                        [Ash Lux] Changes for an aborted build.
				        
				        
                        Changes for Build #5
                        [Ash Lux] Changes for a failed build.
				        
				        
                        Changes for Build #6
                        [Ash Lux] Changes for a not-built build.
				        
				        
                        Changes for Build #7
                        [Ash Lux] Changes for an unstable build.
				        
				        
                        """,
                contentStr);
    }

    @Test
    void testShouldPrintDate() throws Exception {
        content.changesFormat = "%d";

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild4(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        // Date format changed in Java 21, so we have to accomodate the potential narrow no-break space
        // See https://bugs.openjdk.org/browse/JDK-8225245
        assertThat(
                contentStr,
                matchesPattern("""
                        Changes for Build #41
                        Oct 21, 2013, 7:39:00\\hPM
                        Changes for Build #42
                        Oct 21, 2013, 7:39:00\\hPM
                        """));
    }

    @Test
    void testShouldPrintRevision() throws Exception {
        content.changesFormat = "%r";

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild4(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals("""
                Changes for Build #41
                REVISION
                Changes for Build #42
                REVISION
                """, contentStr);
    }

    @Test
    void testShouldPrintPath() throws Exception {
        content.changesFormat = "%p";

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild4(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals("""
                Changes for Build #41
                \tPATH1
                \tPATH2
                \tPATH3
		        
                Changes for Build #42
                \tPATH1
                \tPATH2
                \tPATH3
		        
                """, contentStr);
    }

    @Test
    void testWhenShowPathsIsTrueShouldPrintPath() throws Exception {
        content.showPaths = true;

        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "Changes for a failed build.");

        AbstractBuild currentBuild = createBuild4(Result.SUCCESS, 42, "Changes for a successful build.");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals("""
                Changes for Build #41
                [Ash Lux] Changes for a failed build.
                \tPATH1
                \tPATH2
                \tPATH3
		        
		        
                Changes for Build #42
                [Ash Lux] Changes for a successful build.
                \tPATH1
                \tPATH2
                \tPATH3
		        
		        
                """, contentStr);
    }

    @Test
    void testRegexReplace() throws Exception {
        content.regex = "<defectId>(DEFECT-[0-9]+)</defectId><message>(.*)</message>";
        content.replace = "[$1] $2";
        content.changesFormat = "%m\\n";

        AbstractBuild failureBuild = createBuild(
                Result.FAILURE, 41, "<defectId>DEFECT-666</defectId><message>Changes for a failed build.</message>");

        AbstractBuild currentBuild = createBuild4(
                Result.SUCCESS,
                42,
                "<defectId>DEFECT-666</defectId><message>Changes for a successful build.</message>");
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals(
		        """
                        Changes for Build #41
                        [DEFECT-666] Changes for a failed build.
				        
				        
                        Changes for Build #42
                        [DEFECT-666] Changes for a successful build.
				        
				        
                        """,
                contentStr);
    }

    @Test
    void testShouldPrintDefaultMessageWhenNoChanges() throws Exception {
        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "[DEFECT-666] Changes for a failed build.");

        AbstractBuild currentBuild = createBuildWithNoChanges(Result.SUCCESS, 42);
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals("Changes for Build #41\n"
                + "[Ash Lux] [DEFECT-666] Changes for a failed build.\n"
                + "\n"
                + "\n"
                + "Changes for Build #42\n"
                + ChangesSinceLastBuildMacro.DEFAULT_DEFAULT_VALUE
                + "\n", contentStr);
    }

    @Test
    void testShouldPrintMessageWhenNoChanges() throws Exception {
        content.def = "another default message\n";
        AbstractBuild failureBuild = createBuild(Result.FAILURE, 41, "[DEFECT-666] Changes for a failed build.");

        AbstractBuild currentBuild = createBuildWithNoChanges(Result.SUCCESS, 42);
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals("""
                Changes for Build #41
                [Ash Lux] [DEFECT-666] Changes for a failed build.
		        
		        
                Changes for Build #42
                another default message
		        
                """, contentStr);
    }

    @Test
    void testShouldDefaultToNotEscapeHtml() throws Exception {
        AbstractBuild failureBuild =
                createBuild(Result.FAILURE, 41, "[DEFECT-666] Changes for a failed build. <b>bold</b>");

        AbstractBuild currentBuild = createBuildWithNoChanges(Result.SUCCESS, 42);
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        String contentStr = content.evaluate(currentBuild, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME);

        assertEquals(
		        """
                        Changes for Build #41
                        [Ash Lux] [DEFECT-666] Changes for a failed build. <b>bold</b>
				        
				        
                        Changes for Build #42
                        No changes
				        
                        """,
                contentStr);
    }

    @Test
    void testShouldEscapeHtmlWhenArgumentEscapeHtmlSetToTrue() throws Exception {
        AbstractBuild failureBuild =
                createBuild(Result.FAILURE, 41, "[DEFECT-666] Changes for a failed build. <b>bold</b>");

        AbstractBuild currentBuild = createBuildWithNoChanges(Result.SUCCESS, 42);
        when(currentBuild.getPreviousBuild()).thenReturn(failureBuild);
        when(failureBuild.getNextBuild()).thenReturn(currentBuild);

        final Map<String, String> arguments = Collections.singletonMap("escapeHtml", "true");
        final ArrayListMultimap<String, String> listMultimap = ArrayListMultimap.create();
        listMultimap.put("escapeHtml", "true");
        String contentStr = content.evaluate(
                currentBuild, null, listener, ChangesSinceLastUnstableBuildMacro.MACRO_NAME, arguments, listMultimap);

        assertEquals(
		        """
                        Changes for Build #41
                        [Ash Lux] [DEFECT-666] Changes for a failed build. &lt;b&gt;bold&lt;/b&gt;
				        
				        
                        Changes for Build #42
                        No changes
				        
                        """,
                contentStr);
    }

    private AbstractBuild createBuildWithNoChanges(Result result, int buildNumber) {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet changes1 = createEmptyChangeLog();
        when(build.getChangeSets()).thenReturn(Collections.singletonList(changes1));
        when(build.getNumber()).thenReturn(buildNumber);

        return build;
    }

    private ChangeLogSet createEmptyChangeLog() {
        ChangeLogSet changes = mock(ChangeLogSet.class);
        List<ChangeLogSet.Entry> entries = Collections.emptyList();
        when(changes.isEmptySet()).thenReturn(true);

        return changes;
    }

    private AbstractBuild createBuild(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getResult()).thenReturn(result);
        ChangeLogSet changes1 = createChangeLog(message);
        when(build.getChangeSets()).thenReturn(Collections.singletonList(changes1));
        when(build.getNumber()).thenReturn(buildNumber);

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

    private AbstractBuild createBuild2(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet changes1 = createChangeLog2(message);
        when(build.getResult()).thenReturn(result);
        return build;
    }

    private AbstractBuild createBuild3(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet changes1 = createChangeLog2(message);
        return build;
    }

    private AbstractBuild createBuild4(Result result, int buildNumber, String message) {
        AbstractBuild build = mock(AbstractBuild.class);
        ChangeLogSet changes1 = createChangeLog(message);
        when(build.getChangeSets()).thenReturn(Collections.singletonList(changes1));
        when(build.getNumber()).thenReturn(buildNumber);
        return build;
    }

    private ChangeLogSet createChangeLog2(String message) {
        ChangeLogSet changes = mock(ChangeLogSet.class);
        List<ChangeLogSet.Entry> entries = new LinkedList<>();
        ChangeLogSet.Entry entry = new ChangeLogEntry(message, "Ash Lux");
        entries.add(entry);
        return changes;
    }
}
