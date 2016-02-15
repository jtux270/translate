package org.ovirt.engine.core.utils.ovf.xml;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ovirt.engine.core.uutils.xml.SecureDocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlDocument {

    private String outerXml;
    public XmlNode[] childNodes;

    private Document doc;

    public XmlDocument() {
    }

    public XmlDocument(String xml) throws Exception {
        LoadXml(xml);
    }

    private void LoadXml(String ovfstring) throws Exception {
        // load doc
        DocumentBuilderFactory fact = SecureDocumentBuilderFactory.newDocumentBuilderFactory();
        fact.setNamespaceAware(true);
        DocumentBuilder builder = fact.newDocumentBuilder();
        doc = builder.parse(new InputSource(new StringReader(ovfstring)));

        // initialize all the child nodes
        NodeList list = doc.getElementsByTagName("*");
        childNodes = new XmlNode[list.getLength()];
        for (int i = 0; i < list.getLength(); i++) {
            childNodes[i] = new XmlNode(list.item(i));
        }

        outerXml = ovfstring;
    }

    public XmlNode SelectSingleNode(String string) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            Object o = xPath.evaluate(string, doc, XPathConstants.NODE);
            return new XmlNode((Node) o);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Failed to evaluate xpath: " + string, e);
        }
    }

    public XmlNode SelectSingleNode(String string, XmlNamespaceManager _xmlns) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            xPath.setNamespaceContext(_xmlns);
            Object o = xPath.evaluate(string, doc, XPathConstants.NODE);
            return new XmlNode((Node) o);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Failed to evaluate xpath: " + string, e);
        }
    }

    public XmlNodeList SelectNodes(String string) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            Object o = xPath.evaluate(string, doc, XPathConstants.NODESET);
            return new XmlNodeList((NodeList) o);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Failed to evaluate xpath: " + string, e);
        }
    }

    public XmlNodeList SelectNodes(String string, XmlNamespaceManager _xmlns) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            xPath.setNamespaceContext(_xmlns);
            Object o = xPath.evaluate(string, doc, XPathConstants.NODESET);
            return new XmlNodeList((NodeList) o);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Failed to evaluate xpath: " + string, e);
        }
    }

    public String getOuterXml() {
        return outerXml;
    }
}
