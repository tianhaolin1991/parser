package com.huawei.fossbot.dependency.util.xml;

import com.huawei.fossbot.dependency.bean.Artifact;
import com.huawei.fossbot.dependency.bean.Profile;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;

/**
 * pom解析器
 *
 * @author t30002128
 * @since 2020/05/25
 */
public abstract class PomHandler extends DefaultHandler {
    /**
     * xml reader
     */
    protected XMLReader xmlReader;

    public PomHandler(XMLReader xmlReader) {
        this.xmlReader = xmlReader;
    }

    /**
     * get properties
     */
    public Map<String, String> getProperties() {
        return null;
    }

    /**
     * get managements
     */
    public Map<String, Artifact> getManagements() {
        return null;
    }

    /**
     * get dependencies
     */
    public Map<String, Artifact> getDependencies() {
        return null;
    }

    /**
     * get profiles
     */
    public Map<String, Profile> getProfiles() {
        return null;
    }
}
