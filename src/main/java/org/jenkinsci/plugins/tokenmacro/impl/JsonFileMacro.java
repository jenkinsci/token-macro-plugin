package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONException;

import net.sf.json.JSONObject;

import jenkins.security.MasterToSlaveCallable;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Expands to a xml value(s) from a xml file relative to the workspace root.
 * If xpath evaluates to more than one value then a semicolon separted string is returned.
 */
@Extension
public class JsonFileMacro extends DataBoundTokenMacro {

    public static final Logger LOGGER = Logger.getLogger(JsonFileMacro.class.getName());
    
    private static final String MACRO_NAME = "JSON";

    @Parameter(required=true)
    public String file = null;

    @Parameter(required=true)
    public String path = null;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        return evaluate(context,getWorkspace(context),listener,macroName);
    }

    @Override
    public String evaluate(Run<?,?> run, FilePath workspace, TaskListener listener, String macroName)  throws MacroEvaluationException, IOException, InterruptedException {
        String root = workspace.getRemote();
        return workspace.act(new ReadJSON(root,file,path,run.getCharset()));
    }

    private static class ReadJSON extends MasterToSlaveCallable<String, IOException> {

        private String root;
        private String filename;
        private String pathexpression;
        private Charset charset;

        public ReadJSON(String root, String filename, String path, Charset charset){
            this.root=root;
            this.filename=filename;
            this.pathexpression=path;
            this.charset=charset;
        }

        public String call() throws IOException {
            File file = new File(root, filename);
            String result = "";
            if (file.exists()) {
                try {
                    JSONObject obj = JSONObject.fromObject(FileUtils.readFileToString(file, charset));
                    String[] pathKeys = pathexpression.split("\\.");
                    int count = 0;
                    for(String key : pathKeys) {
                        if(obj.containsKey(key)) {
                            count++;
                            Object obj2 = obj.get(key);
                            if(obj2 instanceof JSONObject) {
                                obj = (JSONObject)obj2;
                            } else {
                                if(count < pathKeys.length) {
                                    result = "Error: Found primitive type at key '".concat(key).concat("' before exhausting path");
                                } else {
                                    result = obj2.toString();
                                }
                                break;
                            }
                        } else {
                            result = "Error: The key '".concat(key).concat("' does not exist in the JSON");
                            break;
                        }
                    }
                }
                catch (IOException e) {
                    result = "Error: ".concat(filename).concat(" - Could not read JSON file.");
                }
                catch (JSONException e) {
                    result = "Error: ".concat(filename).concat(" - JSON not well formed.");
                }
                catch (Throwable e) {
                    LOGGER.log(Level.WARNING, "Unhandled exception during the macro evaluation", e);
                    result = "Error: ".concat(filename).concat(" - '").concat(pathexpression).concat("' invalid syntax or path maybe?");
                }
            }
            else {
                result = "Error: ".concat(filename).concat(" not found");
            }

            return result;
        }
    }
}