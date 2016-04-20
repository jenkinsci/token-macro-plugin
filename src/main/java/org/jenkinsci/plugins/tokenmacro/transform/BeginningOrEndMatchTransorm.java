package org.jenkinsci.plugins.tokenmacro.transform;

import org.jenkinsci.plugins.tokenmacro.Transform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeginningOrEndMatchTransorm extends Transform {
    private String pattern;

    public BeginningOrEndMatchTransorm(String pattern, boolean beginning) {
        if(beginning && !pattern.startsWith("^")) {
            pattern = "^" + pattern;
        } else if(!beginning && !pattern.endsWith("$")) {
            pattern += "$";
        }
        this.pattern = pattern;
    }

    @Override
    public String transform(String input) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(input);
        if(m.find()) {
            input = m.replaceFirst("");
        }
        return input;
    }
}
