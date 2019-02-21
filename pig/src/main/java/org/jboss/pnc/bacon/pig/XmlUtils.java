/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.pig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 7/11/17
 */
public class XmlUtils {
    private static final Logger log = LoggerFactory.getLogger(XmlUtils.class);

    public static XmlToString extract(File xmlFile, String xpathLocator) {
        try {
            NodeList nodeList = extractNodes(xmlFile, xpathLocator);
            return new XmlToString(nodeList);
        } catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
            throw new RuntimeException("Error extracting dependencies from xmlFile: " + xmlFile, e);
        }
    }

    private static NodeList extractNodes(File xmlFile, String xpathLocator) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(xpathLocator);
        return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
    }

    public static List<Node> listNodes(File file, String xpathString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile(xpathString);
            NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            List<Node> resultList = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                resultList.add(nodeList.item(i));
            }
            return resultList;
        } catch (Exception any) {
            throw new RuntimeException("Error searching for matches of " + xpathString + " in " + file.getAbsolutePath(), any);
        }
    }

    public static String getValue(Element parent, String tagName, Map<String, String> properties) {
        List<Element> children = getChildrenWithTagName(parent, tagName);
        int length = children.size();
        if (children.size() > 1) {
            throw new IllegalStateException("Too many elements with name '" + tagName + "'  in " +
                    describe(parent) + ". Expected at most 1, got " + length);
        }

        if (length == 0) {
            return null;
        }

        Element child = (Element) children.get(0);
        String value = child.getTextContent();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            value = value.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        return value.trim();
    }


    public static List<Element> getChildrenWithTagName(Element parent, String tagName) {
        List<Element> elements = new ArrayList<>();
        NodeList descendants = parent.getElementsByTagName(tagName);
        for (int i = 0; i < descendants.getLength(); i++) {
            Node item = descendants.item(i);
            if (item.getParentNode().isSameNode(parent)
                    && item instanceof Element) {
                elements.add((Element) item);
            }
        }
        return elements;
    }

    private static void printPathTo(Node node, StringBuilder output) {
        Node parent = node.getParentNode();
        if (parent != null) {
            printPathTo(parent, output);
            output.append("[").append(indexIn(node)).append("]");
        }
        output.append(".");
        if (node instanceof Element) {
            output.append(((Element) node).getTagName());
        }
    }

    private static int indexIn(Node element) {
        int count = 0;
        while ((element = element.getPreviousSibling()) != null) {
            count++;
        }
        return count;
    }

    private static String describe(Element element) {
        StringBuilder description = new StringBuilder();
        printPathTo(element, description);
        printChildrenTo(element, description);
        return description.toString();
    }

    private static void printChildrenTo(Element element, StringBuilder description) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                description.append(childElement.getTagName() + ":" + childElement.getTextContent() + ", ");
            }
        }
    }

    public static Map<String, String> getProperties(File file) {
        Map<String, String> result = new HashMap<>();
        listNodes(file, "/project/properties/*")
                .stream().filter(n -> n instanceof Element)
                .map(n -> (Element) n)
                .forEach(e -> result.put(e.getTagName(), e.getTextContent().trim()));
        return result;
    }

    public static class XmlToString {
        private final NodeList nodeList;
        private final Set<String> expressionsToSkip = new HashSet<>();

        public XmlToString(NodeList nodeList) {
            this.nodeList = nodeList;
        }

        public XmlToString skipping(String... expression) {
            expressionsToSkip.addAll(Arrays.asList(expression));
            return this;
        }

        public String getContent() {
            if (nodeList.getLength() != 1) {
                throw new RuntimeException("Expecting 1 element, found " + nodeList.getLength());
            }
            Node item = nodeList.item(0);
            return item.getTextContent();
        }

        public String asString() throws TransformerException {
            StringBuilder result = new StringBuilder();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            for (int i = 0; i < nodeList.getLength(); ++i) {
                StringWriter writer = new StringWriter();
                StreamResult streamResult = new StreamResult(writer);
                DOMSource source = new DOMSource();
                source.setNode(nodeList.item(i));
                transformer.transform(source, streamResult);
                String element = writer.toString();
                if (expressionsToSkip.stream().noneMatch(element::contains)) {
                    result.append(element).append("\n");
                }
            }

            return result.toString();
        }
    }
}
