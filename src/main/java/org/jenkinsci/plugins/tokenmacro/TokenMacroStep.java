package org.jenkinsci.plugins.tokenmacro;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Set;

public class TokenMacroStep extends Step {

    private final @Nonnull String stringWithMacro;

    @DataBoundConstructor
    public TokenMacroStep(@Nonnull String stringWithMacro) {
        this.stringWithMacro = stringWithMacro;
    }

    public String getStringWithMacro() {
        return stringWithMacro;
    }

    @Override public StepExecution start(StepContext context) throws Exception {
        return new Execution(stringWithMacro, context);
    }

    private static class Execution extends SynchronousNonBlockingStepExecution<String> {
        private static final long serialVersionUID = 1L;

        @SuppressFBWarnings(value="SE_TRANSIENT_FIELD_NOT_RESTORED", justification="Only used when starting.")
        private transient final String stringWithMacro;

        Execution(String stringWithMacro, StepContext context) {
            super(context);
            this.stringWithMacro = stringWithMacro;
        }

        @Override protected String run() throws Exception {
            return TokenMacro.expand(getContext().get(Run.class), getContext().get(FilePath.class), getContext().get(TaskListener.class), stringWithMacro);
        }
    }

    @Extension public static class DescriptorImpl extends StepDescriptor {

        @Override public String getFunctionName() {
            return "tm";
        }

        @Override public String getDisplayName() {
            return "Expand a string containing macros";
        }

        @Override public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, TaskListener.class);
        }
    }
}
