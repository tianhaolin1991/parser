package com.huawei.fossbot.dependency.bean;

public enum  AnalyzerType {
    MAVEN("pom.xml"),
    GRADLE("build.gradle"),
    GO("go.mod");

    private String profile;

    AnalyzerType(String profile){
        this.profile = profile;
    }

    public String getProfile(){
        return this.profile;
    }
}
