package com.huawei.fossbot.dependency.util.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;

/**
 * parent标签解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class ParentHandler extends DefaultHandler {

    private XMLReader xmlReader;
    private PomHandler pomHandler;
    private String key;
    private String value = "";

    public ParentHandler(XMLReader xmlReader, PomHandler pomHandler) {
        this.pomHandler = pomHandler;
        this.xmlReader = xmlReader;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("groupId".equals(qName)) {
            key = "project.parent.groupId";
            return;
        }
        if ("artifactId".equals(qName)) {
            key = "project.parent.artifactId";
            return;
        }
        if ("version".equals(qName)) {
            key = "project.parent.version";
            return;
        }
        if ("relativePath".equals(qName)) {
            key = "parent.relativePath";
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("parent".equals(qName)) {
            xmlReader.setContentHandler(pomHandler);
            key = null;
            value = "";
        } else {
            pomHandler.getProperties().putIfAbsent(key, value);
            value = "";
            key = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (key != null) {
            value = value + new String(ch, start, length).trim();
        }
    }

}
