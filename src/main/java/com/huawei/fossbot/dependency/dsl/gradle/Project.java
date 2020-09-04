package com.huawei.fossbot.dependency.dsl.gradle;

import com.huawei.fossbot.dependency.bean.Artifact;
import groovy.lang.Closure;

/**
 * denote build.gradle
 *
 * @author t30002128
 * @since 2020/06/04
 */
public class Project {
    private static String DEFAULT_BUILD_FILE = "build.gradle";
    private Artifact artifact;
    private DependencyHandler dependencies = new DependencyHandler();
    private RepositoryHandler repositories = new RepositoryHandler();
    private Project rootProject;

    public Project(Settings settings) {
        this.rootProject = this;
        this.artifact = new Artifact();
        this.artifact.setGroupId("");
        this.artifact.setVersion("");
        this.artifact.setArtifactId(settings.getRootProject().getName());
    }

    public Project(Project rootProject){
        this.rootProject = rootProject;
    }

    public String getGroup() {
        return artifact.getGroupId();
    }

    public String getName() {
        return artifact.getArtifactId();
    }

    public void setName(String name) {
        artifact.setArtifactId(name);
    }

    public void dependencies(Closure closure) {
        closure.setDelegate(dependencies);
        closure.call();
    }

    public RepositoryHandler getRepo(){
        return this.repositories;
    }

    public Artifact getArtifact(){
        return this.artifact;
    }


}

