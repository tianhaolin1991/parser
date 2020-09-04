package com.huawei.fossbot.dependency.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置文件(single),用于表示原始的配置文件,没有依赖关系
 * 用于maven自研解析
 *
 * @since 2020/06/03
 */
public class Profile {
    private String id;
    private boolean isDefault = false;
    private Map<String, String> activation = new LinkedHashMap<>();
    private Map<String, Artifact> dependencies = new LinkedHashMap<>();
    private Map<String, Artifact> managements = new LinkedHashMap<>();
    private Map<String, String> properties = new LinkedHashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return 依赖管理信息
     */
    public Map<String, Artifact> getManagement() {
        return managements;
    }

    public void setManagements(Map<String, Artifact> managements) {
        this.managements = managements;
    }

    public Map<String, Artifact> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, Artifact> dependencies) {
        this.dependencies = dependencies;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Map<String, String> getActivation() {
        return this.activation;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
