package com.huawei.fossbot.dependency.util.xml;

import org.xml.sax.XMLReader;

/**
 * profile management解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class ProfileManagementHandler extends DependencyHandler {

    public ProfileManagementHandler(XMLReader xmlReader, PomHandler preHandler) {
        super(xmlReader, preHandler);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (qName) {
            case "dependencyManagement":
                xmlReader.setContentHandler(preHandler);
                break;
            case "dependency":
                ((ProfileHandler) preHandler).getProfile()
                        .getManagement().putIfAbsent(artifact.getKey(), artifact);
                break;
            default:
                super.endElement(uri, localName, qName);
                break;
        }
    }
}
