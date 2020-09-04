package com.huawei.fossbot.dependency.dsl.gradle;

import com.huawei.fossbot.dependency.bean.Artifact;
import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyHandler {
    private List<Artifact> dependencies = new ArrayList<>();
    private Map<String,DependencyConfiguration> configurations = new HashMap<>();

    public void add(String type,String group,String name,String version){
        Artifact dependency = new Artifact();
        dependency.setGroupId(group);
        dependency.setArtifactId(name);
        dependency.setVersion(version);
        dependencies.add(dependency);
    }

    public void add(String type, String group, String name, String version, Closure closure){
        Artifact dependency = new Artifact();
        dependency.setGroupId(group);
        dependency.setArtifactId(name);
        dependency.setVersion(version);
        dependencies.add(dependency);
    }

    public void addConfigurations(List<DependencyConfiguration> configuration){
        for (DependencyConfiguration conf : configuration) {
            configurations.putIfAbsent(conf.getName(),conf);
        }
    }

    public Map<String,DependencyConfiguration> getConfigurations(){
        return this.configurations;
    }
}
