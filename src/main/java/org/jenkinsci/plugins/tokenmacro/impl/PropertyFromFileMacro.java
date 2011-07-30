
package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Expands to a property from a property file relative to the workspace root.
 */
@Extension
public class PropertyFromFileMacro extends DataBoundTokenMacro {

    @Parameter(required=true)
    public String file = null;

    @Parameter(required=true)
    public String property = null;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("PROPFILE");
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        String root = context.getWorkspace().getRemote();
        return context.getWorkspace().act(new ReadProperty(root,file,property));
    }

    private static class ReadProperty implements Callable<String,IOException> {

        private String root;
        private String filename;
        private String propertyname;
        
        public ReadProperty(String root, String filename, String property){
            this.root=root;
            this.filename=filename;
            this.propertyname=property;
        }
        
        public String call() throws IOException {
            Properties props = new Properties();
            props.load(new BufferedReader(new FileReader(new File(root,filename))));

            if(props.containsKey(propertyname)){
                return props.getProperty(propertyname);
            }
            return "";
        }
    }
}
