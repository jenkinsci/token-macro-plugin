package org.jenkinsci.plugins.tokenmacro;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Created by acearl on 2/24/2016.
 */
public abstract class Transform {
    public abstract String transform (@NonNull String input) throws MacroEvaluationException;
}
