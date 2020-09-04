package com.huawei.fossbot.dependency.util.xml;

import org.xml.sax.XMLReader;

/**
 * management解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class ManagementHandler extends DependencyHandler {
    public ManagementHandler(XMLReader xmlReader, PomHandler pomHandler) {
        super(xmlReader, pomHandler);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (qName) {
            case "dependencyManagement":
                xmlReader.setContentHandler(preHandler);
                break;
            case "dependency":
                preHandler.getManagements().putIfAbsent(artifact.getKey(), artifact);
                break;
            default:
                super.endElement(uri, localName, qName);
                break;
        }
    }

}
