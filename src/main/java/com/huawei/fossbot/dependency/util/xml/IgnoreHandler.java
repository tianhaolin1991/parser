package com.huawei.fossbot.dependency.util.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * 忽略标签
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class IgnoreHandler extends PomHandler {
    private int depth = 1;
    private PomHandler preHandler;

    public IgnoreHandler(XMLReader xmlReader, PomHandler pomHandler) {
        super(xmlReader);
        this.preHandler = pomHandler;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        depth++;
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        depth--;
        if (0 == depth) {
            xmlReader.setContentHandler(preHandler);
            depth = 1;
        }
    }

}
