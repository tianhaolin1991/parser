package com.huawei.fossbot.dependency.util.xml;

import org.xml.sax.XMLReader;

/**
 * profile dependencies解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class ProfileDependencyHandler extends DependencyHandler {

    public ProfileDependencyHandler(XMLReader xmlReader, PomHandler preHandler) {
        super(xmlReader, preHandler);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (qName) {
            case "dependencies":
                xmlReader.setContentHandler(preHandler);
                break;
            case "dependency":
                ((ProfileHandler) preHandler).getProfile()
                        .getDependencies().putIfAbsent(artifact.getKey(), artifact);
                break;
            default:
                super.endElement(uri, localName, qName);
                break;
        }
    }
}
