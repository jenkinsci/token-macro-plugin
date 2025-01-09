package org.jenkinsci.plugins.tokenmacro.impl;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.security.MasterToSlaveCallable;

import org.jenkinsci.plugins.tokenmacro.WorkspaceDependentMacro;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.xpath.*;
import javax.xml.parsers.*;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Expands to a xml value(s) from a xml file relative to the workspace root.
 * If xpath evaluates to more than one value then a semicolon separted string is returned.
 */
@Extension
public class XmlFileMacro extends WorkspaceDependentMacro {

    public static final Logger LOGGER = Logger.getLogger(XmlFileMacro.class.getName());
    
    private static final String MACRO_NAME = "XML";

    @Parameter(required=true)
    public String file = null;

    @Parameter(required=true)
    public String xpath = null;

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
    public Callable<String, IOException> getCallable(Run<?,?> run, String root, TaskListener listener) {
        return new ReadXML(root, file, xpath);
    }

    private static class ReadXML extends MasterToSlaveCallable<String, IOException> {

        private String root;
        private String filename;
        private String xpathexpression;

        private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
        private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
        private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
        private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

        public ReadXML(String root, String filename, String xpath){
            this.root=root;
            this.filename=filename;
            this.xpathexpression=xpath;
        }

        private DocumentBuilderFactory createFactory() {
            DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
            HashMap<String, Boolean> features = new HashMap<>();
            features.put(DISALLOW_DOCTYPE_DECL, true);
            features.put(EXTERNAL_GENERAL_ENTITIES, false);
            features.put(EXTERNAL_PARAMETER_ENTITIES, false);
            features.put(LOAD_EXTERNAL_DTD, false);

            for(Map.Entry<String, Boolean> feature : features.entrySet()) {
                try {
                    dFactory.setFeature(feature.getKey(), feature.getValue());
                } catch(ParserConfigurationException e) {
                    LOGGER.log(Level.INFO, "Could not enable/disable feature: " + feature);
                }
            }

            HashMap<String, String> attributes = new HashMap<>();
            attributes.put(XMLConstants.FEATURE_SECURE_PROCESSING, "true");
            attributes.put(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            attributes.put(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            attributes.put(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            for(Map.Entry<String, String> attribute : attributes.entrySet()) {
                try {
                    dFactory.setAttribute(attribute.getKey(), attribute.getValue());
                } catch(IllegalArgumentException e) {
                    LOGGER.log(Level.INFO, "Could not set attribute: " + attribute);
                }
            }

            dFactory.setXIncludeAware(false);
            dFactory.setExpandEntityReferences(false);
            return dFactory;
        }

        public String call() {
            File file = new File(root, filename);
            String result = "";
            if (file.exists()) {
                try {
                    DocumentBuilderFactory dFactory = createFactory();

                    EntityResolver noop = new EntityResolver() {
                        @Override
                        public InputSource resolveEntity(String publicId, String systemId) {
                            return new InputSource(new StringReader(""));
                        }
                    };
                    DocumentBuilder builder = dFactory.newDocumentBuilder();
                    builder.setEntityResolver(noop);
                    Document doc = builder.parse(file.toString());
                    XPath xpathInstance = XPathFactory.newInstance().newXPath();
                    XPathExpression expr = xpathInstance.compile(xpathexpression);

                    Object res = expr.evaluate(doc, XPathConstants.NODESET);
                    NodeList nodes = (NodeList) res;
                    for (int i = 0; i < nodes.getLength(); i++) {
                        result = result.concat(nodes.item(i).getNodeValue()).concat(";");
                    }

                    result = result.substring(0, result.length() - 1); // trim the last ';'
                }
                catch (IOException e) {
                    result = "Error: ".concat(filename).concat(" - Could not read XML file.");
                }
                catch (SAXException e) {
                    result = "Error: ".concat(filename).concat(" - XML not well formed.");
                }
                catch (Throwable e) {
                    LOGGER.log(Level.WARNING, "Unhandled exception during the macro evaluation", e);
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