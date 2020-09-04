package com.huawei.fossbot.dependency.util.xml;

import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.bean.Profile;
import com.huawei.fossbot.dependency.parser.java.MavenParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * project解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class ProjectHandler extends PomHandler {
    private Map<String, DefaultHandler> handlerMap = new HashMap<>();
    private DefaultHandler ignoreHandler;

    private Map<String, String> properties = new HashMap<>();
    private Map<String, Artifact> managements = new LinkedHashMap<>();
    private Map<String, Artifact> dependencies = new LinkedHashMap<>();
    private Map<String, Profile> profiles = new LinkedHashMap<>();
    private String key;

    public ProjectHandler(XMLReader xmlReader) {
        super(xmlReader);
        this.ignoreHandler = new IgnoreHandler(xmlReader, this);
        handlerMap.put("parent", new ParentHandler(xmlReader, this));
        handlerMap.put("properties", new PropertyHandler(xmlReader, this));
        handlerMap.put("dependencyManagement", new ManagementHandler(xmlReader, this));
        handlerMap.put("dependencies", new DependencyHandler(xmlReader, this));
        handlerMap.put("profiles", new ProfileHandler(xmlReader, this));
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (changeHandler(qName)) {
            xmlReader.setContentHandler(handlerMap.get(qName));
        } else if (validElement(qName)) {
            switch (qName) {
                case ("groupId"):
                    key = "project.groupId";
                    break;
                case ("artifactId"):
                    key = "project.artifactId";
                    break;
                case ("version"):
                    key = "project.version";
                    break;
            }
        } else {
            xmlReader.setContentHandler(ignoreHandler);
        }
    }

    private boolean changeHandler(String qName) {
        return handlerMap.containsKey(qName);
    }

    private boolean validElement(String qName) {
        return qName.equals("groupId") || qName.equals("artifactId")
                || qName.equals("version") || qName.equals("project");
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (key != null) {
            String value = new String(ch, start, length).trim();
            properties.putIfAbsent(key, value);
            key = null;
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public Map<String, Artifact> getManagements() {
        return this.managements;
    }

    @Override
    public Map<String, Artifact> getDependencies() {
        return this.dependencies;
    }

    @Override
    public Map<String, Profile> getProfiles() {
        return this.profiles;
    }

}