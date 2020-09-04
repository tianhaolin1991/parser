package com.huawei.fossbot.dependency.dsl.gradle;


import groovy.lang.Binding;

import java.util.HashMap;

public class GradleBinding extends Binding {

    public GradleBinding(){
        super();
        HashMap<String, String> pluginMap = new HashMap<>();
        pluginMap.put("java","com.huawei.fossbot.dependency.dsl.gradle.JavaPlugin");
        super.setProperty("plugins",pluginMap);
    }
}
