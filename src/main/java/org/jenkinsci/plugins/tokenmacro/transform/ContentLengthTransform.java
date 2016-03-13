package org.jenkinsci.plugins.tokenmacro.transform;

import org.jenkinsci.plugins.tokenmacro.Transform;

/**
 * Created by acearl on 2/24/2016.
 */
public class ContentLengthTransform extends Transform {
    @Override
    public String transform(String input) {
        return String.valueOf(input.length());
    }
}
