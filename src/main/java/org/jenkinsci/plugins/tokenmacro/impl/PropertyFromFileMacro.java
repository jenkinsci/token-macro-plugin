package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import java.io.Closeable;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        final FilePath workspace = getWorkspace(context, macroName);
        String root = workspace.getRemote();
        return workspace.act(new ReadProperty(root,file,property));
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
            File file = new File(root, filename);
            String propertyValue = "";
            if (file.exists()) {
                Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                Closeable resource = reader;    
                try {
                    BufferedReader bReader = new BufferedReader(reader);
                    resource = bReader;
                    props.load(bReader);
                    if(props.containsKey(propertyname)){
                        propertyValue = props.getProperty(propertyname);
                    } 
                }
                catch (IOException e) {
                    propertyValue = "Error reading ".concat(filename);
                }
                finally {
                    resource.close();
                }
            }
            else {
                propertyValue = filename.concat(" not found");
            }
            
            return propertyValue;
        }
    }
}
