package com.huawei.fossbot.dependency.util.xml;

import com.huawei.fossbot.dependency.bean.Artifact;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;

import java.util.HashMap;
import java.util.Map;

/**
 * dependency解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class DependencyHandler extends PomHandler {
    /**
     * pre handler
     */
    protected PomHandler preHandler;
    /**
     * artifact
     */
    protected Artifact artifact = new Artifact();
    /**
     * key
     */
    protected String key;
    /**
     * value
     */
    protected String value = "";
    /**
     * isExclusion
     */
    protected boolean isExclusion = false;
    /**
     * exclusions
     */
    protected Map<String, String> exclusion = new HashMap<>();

    public DependencyHandler(XMLReader xmlReader, PomHandler preHandler) {
        super(xmlReader);
        this.preHandler = preHandler;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        switch (qName) {
            case "dependency":
                artifact = new Artifact();
                break;
            case "exclusions":
                isExclusion = true;
                break;
            case "exclusion":
                exclusion.clear();
                break;
            case "groupId":
            case "artifactId":
            case "version":
            case "scope":
            case "optional":
            case "systemPath":
                key = qName;
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (qName) {
            case "dependencies":
                xmlReader.setContentHandler(preHandler);
                break;
            case "dependency":
                preHandler.getDependencies().putIfAbsent(artifact.getKey(), artifact);
                break;
            case "exclusions":
                isExclusion = false;
                break;
            case "exclusion":
                artifact.getExcludes().add(exclusion.get("groupId") + ":" + exclusion.get("artifactId"));
                exclusion.clear();
                break;
            case "groupId":
                if (isExclusion) {
                    exclusion.put(key, value);
                } else {
                    artifact.setGroupId(value);
                }
                break;
            case "artifactId":
                if (isExclusion) {
                    exclusion.put(key, value);
                } else {
                    artifact.setArtifactId(value);
                }
                break;
            case "version":
                artifact.setVersion(value);
                break;
            case "scope":
                artifact.setScope(value);
                break;
            case "optional":
                artifact.setOptional(Boolean.parseBoolean(value));
                break;
            case "systemPath":
                artifact.setSource(value);
                break;
        }
        value = "";
        key = null;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (key != null) {
            this.value = this.value + new String(ch, start, length).trim();
        }
    }
}
