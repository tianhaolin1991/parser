package com.huawei.fossbot.dependency.dsl.gradle;

import java.util.ArrayList;
import java.util.List;

public class JavaPlugin {
    private final static String NAME = "java";
    private List<DependencyConfiguration> configurations;
    public JavaPlugin(){
        initJavaPlugin();
    }

    public String getName(){
        return NAME;
    }
    public List<DependencyConfiguration> getConfigurations() {
        return this.configurations;
    }

    private void initJavaPlugin() {
        fillConfigurations();
    }

    private void fillConfigurations() {
        configurations = new ArrayList<>();
        DependencyConfiguration compile = new DependencyConfiguration("compile");
        configurations.add(compile);
        DependencyConfiguration compileOnly = new DependencyConfiguration("compile");
        configurations.add(compileOnly);
        DependencyConfiguration implementation = new DependencyConfiguration("implementation");
        configurations.add(implementation);
        DependencyConfiguration runtimeOnly = new DependencyConfiguration("runtimeOnly");
        configurations.add(runtimeOnly);
        DependencyConfiguration runtime = new DependencyConfiguration("runtime");
        configurations.add(runtime);
        DependencyConfiguration compileClasspath = new DependencyConfiguration("compileClasspath");
        configurations.add(compileClasspath);
        DependencyConfiguration runtimeClasspath = new DependencyConfiguration("compileClasspath");
        configurations.add(runtimeClasspath);
        DependencyConfiguration testCompile = new DependencyConfiguration("testCompile");
        configurations.add(testCompile);
        DependencyConfiguration testRuntimeOnly = new DependencyConfiguration("testRuntimeOnly");
        configurations.add(testRuntimeOnly);
        DependencyConfiguration testRuntime = new DependencyConfiguration("testRuntime");
        configurations.add(testRuntime);
        DependencyConfiguration testImplementation = new DependencyConfiguration("testImplementation");
        configurations.add(testImplementation);
        DependencyConfiguration testCompileOnly = new DependencyConfiguration("testCompileOnly");
        configurations.add(testCompileOnly);
        DependencyConfiguration testRuntimeClasspath = new DependencyConfiguration("testRuntimeClasspath");
        configurations.add(testRuntimeClasspath);
        DependencyConfiguration testCompileClasspath = new DependencyConfiguration("testCompileClasspath");
        configurations.add(testCompileClasspath);
        implementation.extendsFrom(compile);
        runtime.extendsFrom(compile);
        compileClasspath.extendsFrom(compileOnly,implementation);
        runtimeClasspath.extendsFrom(implementation,runtimeOnly,runtime);
        testCompile.extendsFrom(compile);
        testRuntimeOnly.extendsFrom(runtimeOnly);
        testRuntime.extendsFrom(runtime);
        testImplementation.extendsFrom(implementation,testCompile);
        testRuntimeClasspath.extendsFrom(testRuntimeOnly,testRuntime,testImplementation);
        testCompileClasspath.extendsFrom(testImplementation,testCompileOnly);
    }
}
