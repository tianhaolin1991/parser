package com.huawei.fossbot.dependency.bean;

/**
 * 版本管理文件类型
 *
 * @since 2020/06/03
 */
public enum Type {
    MAVEN("pom.xml"),
    GRADLE("build.gradle");

    private String profile;

    Type(String profile) {
        this.profile = profile;
    }

    public String getProfile() {
        return this.profile;
    }
}
