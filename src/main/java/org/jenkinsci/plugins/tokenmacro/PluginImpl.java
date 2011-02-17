package org.jenkinsci.plugins.tokenmacro;

import hudson.ExtensionList;
import hudson.Plugin;

/**
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {
    // exposed for Jelly binding
    public ExtensionList<TokenMacro> getAllTokenMacros() {
        return TokenMacro.all();
    }
}
