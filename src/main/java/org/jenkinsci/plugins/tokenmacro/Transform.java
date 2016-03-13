package org.jenkinsci.plugins.tokenmacro;

import javax.annotation.Nonnull;

/**
 * Created by acearl on 2/24/2016.
 */
public abstract class Transform {
    public abstract String transform (@Nonnull String input) throws MacroEvaluationException;
}
