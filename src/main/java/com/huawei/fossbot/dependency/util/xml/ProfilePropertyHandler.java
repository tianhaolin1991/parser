package com.huawei.fossbot.dependency.util.xml;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * profile property解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class ProfilePropertyHandler extends PropertyHandler {

    public ProfilePropertyHandler(XMLReader xmlReader, PomHandler pomHandler) {
        super(xmlReader, pomHandler);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if ("properties".equals(qName)) {
            this.value = "";
            this.key = null;
            xmlReader.setContentHandler(pomHandler);
        } else {
            ((ProfileHandler) pomHandler).getProfile().getProperties().put(key, value);
            this.value = "";
            this.key = null;
        }
    }
}
