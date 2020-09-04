package com.huawei.fossbot.dependency.dsl.gradle;

import java.util.HashMap;
import java.util.Map;

public class DependencyConfiguration{
    private String name;
    private Map<String,DependencyConfiguration> extendsFrom = new HashMap<>();
    private Map<String,DependencyConfiguration> children = new HashMap<>();

    public DependencyConfiguration(String name){
        this.name = name;
    }

    public void extendsFrom(DependencyConfiguration... configuration){
        for (DependencyConfiguration superConfig : configuration) {
            superConfig.addChild(this);
            this.extendsFrom.put(this.name,superConfig);
        }
    }

    public void addChild(DependencyConfiguration child){
        this.children.put(child.name,child);
    }

    public String getName(){
        return this.name;
    }

}
