package com.huawei.fossbot.dependency.util.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * property解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class PropertyHandler extends PomHandler {

    /**
     * pomHandler
     */
    protected PomHandler pomHandler;
    /**
     * key
     */
    protected String key;
    /**
     * value
     */
    protected String value = "";

    public PropertyHandler(XMLReader xmlReader, PomHandler pomHandler) {
        super(xmlReader);
        this.pomHandler = pomHandler;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        key = qName;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("properties".equals(qName)) {
            this.value = "";
            this.key = null;
            xmlReader.setContentHandler(pomHandler);
        } else {
            pomHandler.getProperties().put(key, value);
            this.value = "";
            this.key = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (key != null) {
            this.value = value + new String(ch, start, length).trim();
        }
    }
}
