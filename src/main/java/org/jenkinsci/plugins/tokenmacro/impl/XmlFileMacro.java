package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import java.io.Closeable;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.w3c.dom.*;
import javax.xml.xpath.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Expands to a xml value(s) from a xml file relative to the workspace root.
 * If xpath evaluates to more than one value then a semicolon separted string is returned.
 */
@Extension
public class XmlFileMacro extends DataBoundTokenMacro {

    @Parameter(required=true)
    public String file = null;

    @Parameter(required=true)
    public String xpath = null;

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("XML");
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName) throws MacroEvaluationException, IOException, InterruptedException {
        String root = context.getWorkspace().getRemote();
        return context.getWorkspace().act(new ReadXML(root,file,xpath));
    }

    private static class ReadXML implements Callable<String,IOException> {

        private String root;
        private String filename;
        private String xpathexpression;

        public ReadXML(String root, String filename, String xpath){
            this.root=root;
            this.filename=filename;
            this.xpathexpression=xpath;
        }

        public String call() throws IOException {
            File file = new File(root, filename);
            String result = "";
            if (file.exists()) {
                try {
                    DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = dFactory.newDocumentBuilder();
                    Document doc = builder.parse(file.toString());
                    XPath xpathInstance = XPathFactory.newInstance().newXPath();
                    XPathExpression expr = xpathInstance.compile(xpathexpression);

                    Object res = expr.evaluate(doc, XPathConstants.NODESET);
                    NodeList nodes = (NodeList) res;
                    for (int i = 0; i < nodes.getLength(); i++) {
                        result = result.concat(nodes.item(i).getNodeValue().toString()).concat(";");
                    }

                    result = result.substring(0, result.length() - 1); // trim the last ';'
                }
                catch (IOException e) {
                    result = "Error: ".concat(filename).concat(" - Could not read XML file.");
                }
                catch (SAXException e) {
                    result = "Error: ".concat(filename).concat(" - XML not well formed.");
                }
                catch (Exception e) {
                    result = "Error: ".concat(filename).concat(" - '").concat(xpathexpression).concat("' invalid syntax or path maybe?");
                }
            }
            else {
                result = "Error: ".concat(filename).concat(" not found");
            }

            return result;
        }
    }
}