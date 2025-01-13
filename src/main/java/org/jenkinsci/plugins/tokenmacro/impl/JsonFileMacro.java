package org.jenkinsci.plugins.tokenmacro.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import hudson.remoting.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONException;

import net.sf.json.JSONObject;

import jenkins.security.MasterToSlaveCallable;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.WorkspaceDependentMacro;

/**
 * Expands to a xml value(s) from a xml file relative to the workspace root.
 * If xpath evaluates to more than one value then a semicolon separted string is returned.
 */
@Extension
public class JsonFileMacro extends WorkspaceDependentMacro {

    public static final Logger LOGGER = Logger.getLogger(JsonFileMacro.class.getName());
    
    private static final String MACRO_NAME = "JSON";

    @Parameter(required=true)
    public String file = null;

    @Parameter
    @SuppressFBWarnings(value="PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification="Retain API compatibility.")
    public String path = null;

    @Parameter
    public String expr = null;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals(MACRO_NAME);
    }

    @Override
    public List<String> getAcceptedMacroNames() {
        return Collections.singletonList(MACRO_NAME);
    }

    @Override
    public Callable<String, IOException> getCallable(Run<?,?> run, String root, TaskListener listener) {
        // jsonPath takes precedence
        if (path != null && expr != null) {
            path = null;
        }
        return new ReadJSON(root,file,path,expr,run.getCharset());
    }

    private static class ReadJSON extends MasterToSlaveCallable<String, IOException> {

        private String root;
        private String filename;
        private String expr;
        private String path;
        private Charset charset;

        public ReadJSON(String root, String filename, String path, String expr, Charset charset){
            this.root=root;
            this.filename=filename;
            this.path=path;
            this.charset=charset;
            this.expr=expr;
        }

        public String extractWithPath(File file) {
            String result = "";
            try {
                JSONObject obj = JSONObject.fromObject(FileUtils.readFileToString(file, charset));
                String[] pathKeys = path.split("\\.");
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
            } catch (IOException e) {
                result = "Error: ".concat(filename).concat(" - Could not read JSON file.");
            } catch (JSONException e) {
                result = "Error: ".concat(filename).concat(" - JSON not well formed.");
            } catch (Throwable e) {
                LOGGER.log(Level.WARNING, "Unhandled exception during the macro evaluation", e);
                result = "Error: ".concat(filename).concat(" - '").concat(path).concat("' invalid syntax or path maybe?");
            }
            return result;
        }

        public String extractWithJsonPath(File file) {
            String result;
            try {
                DocumentContext jsonContext = JsonPath.parse(file);
                result = jsonContext.read(expr);
            } catch (IOException e) {
                result = "Error: ".concat(filename).concat(" - Could not read JSON file");
            }
            return result;
        }

        public String call() throws IOException {
            if (path == null && expr == null) {
                return "You must specify the path or expr parameter";
            }
            File file = new File(root, filename);
            String result = "";
            if (file.exists()) {
                if(path != null) {
                    result = extractWithPath(file);
                } else if(expr != null) {
                    result = extractWithJsonPath(file);
                }
            } else {
                result = "Error: ".concat(filename).concat(" not found");
            }

            return result;
        }
    }
}
