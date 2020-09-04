package com.huawei.fossbot.dependency.util.xml;

import com.huawei.fossbot.dependency.bean.Profile;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * profile解析器
 *
 * @author t30002128
 * @since 2020/05/30
 */
public class ProfileHandler extends PomHandler {

    private final PomHandler preHandler;
    private Profile profile;
    private String key;
    private String value = "";
    private IgnoreHandler ignoreHandler;
    private PomHandler dependencyHandler;
    private PomHandler managementHandler;
    private PomHandler propertiesHandler;

    public ProfileHandler(XMLReader xmlReader, PomHandler pomHandler) {
        super(xmlReader);
        this.preHandler = pomHandler;
        this.ignoreHandler = new IgnoreHandler(xmlReader, this);
        this.dependencyHandler = new ProfileDependencyHandler(xmlReader, this);
        this.managementHandler = new ProfileManagementHandler(xmlReader, this);
        this.propertiesHandler = new ProfilePropertyHandler(xmlReader, this);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case "profile":
                profile = new Profile();
                break;
            case "activeByDefault":
            case "jdk":
            case "os":
            case "file":
            case "property":
            case "id":
                key = qName;
                value = "";
                break;
            case "name":
            case "value":
            case "family":
            case "version":
                key = key + "." + qName;
                value = "";
                break;
            case "dependencies":
                key = null;
                value = "";
                xmlReader.setContentHandler(this.dependencyHandler);
                break;
            case "dependencyManagement":
                key = null;
                value = "";
                xmlReader.setContentHandler(this.managementHandler);
                break;
            case "properties":
                key = null;
                value = "";
                xmlReader.setContentHandler(this.propertiesHandler);
                break;
            case "activation":
                key = null;
                value = "";
                break;
            default:
                key = null;
                value = "";
                xmlReader.setContentHandler(this.ignoreHandler);
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "profiles":
                xmlReader.setContentHandler(this.preHandler);
                break;
            case "profile":
                preHandler.getProfiles().put(this.profile.getId(), this.profile);
                break;
        }
        if (key != null) {
            switch (key) {
                case "activeByDefault":
                    this.profile.setDefault(Boolean.parseBoolean(value));
                    break;
                case "id":
                    this.profile.setId(value);
                    break;
                case "jdk":
                    this.profile.getActivation().put(key, value);
                    break;
                case "os.name":
                case "os.family":
                case "os.arch":
                case "os.version":
                case "property.name":
                case "property.value":
                case "file.exists":
                case "file.missing":
                    this.profile.getActivation().put(key, value);
                    key = key.split("\\.")[0];
                    break;
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (key != null) {
            value = value + new String(ch, start, length).trim();
        }
    }

    public Profile getProfile() {
        return this.profile;
    }
}
