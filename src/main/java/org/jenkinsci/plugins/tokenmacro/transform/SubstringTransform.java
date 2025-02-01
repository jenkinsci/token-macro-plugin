package org.jenkinsci.plugins.tokenmacro.transform;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.Transform;

/**
 * Created by acearl on 2/25/2016.
 */
public class SubstringTransform extends Transform {

    private int offset;
    private int length;

    public SubstringTransform(int offset, int length) {
        this.offset = offset;
        this.length = length;
    }

    @Override
    public String transform(@NonNull String input) throws MacroEvaluationException {
        if (offset > input.length()) {
            throw new MacroEvaluationException(String.format("Offset given (%d) is larger than the string", offset));
        }

        if (offset < 0) {
            offset = input.length() + offset;
        }

        if (length == Integer.MAX_VALUE) {
            input = input.substring(offset);
        } else {
            if (length < 0) {
                length = input.length() + length - offset;
            }

            if (offset + length > input.length()) {
                throw new MacroEvaluationException(String.format(
                        "Incorrect offset or length: input length is %d and offset end is %d",
                        input.length(), offset + length));
            }

            input = input.substring(offset, offset + length);
        }

        return input;
    }
}
