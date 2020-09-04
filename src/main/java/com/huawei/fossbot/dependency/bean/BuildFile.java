package com.huawei.fossbot.dependency.bean;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Build File:表示项目构建的配置文件
 * 包括module和children
 * 用于展示依赖树
 *
 * @since 2020/06/03
 */
public class BuildFile {
    private Path path;
    private Artifact artifact;
    private BuildFile parent;
    private List<BuildFile> children = new ArrayList<>();
    private BuildFile root;
    private List<BuildFile> modules = new ArrayList<>();

    public BuildFile(Path path, Artifact artifact) {
        this.path = path;
        this.artifact = artifact;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public BuildFile getParent() {
        return parent;
    }

    public void setParent(BuildFile parent) {
        this.parent = parent;
    }

    public List<BuildFile> getChildren() {
        return this.children;
    }

    /**
     * @param child 子项目的构建文件
     */
    public void addChild(BuildFile child) {
        this.children.add(child);
        child.setParent(this);
    }

    /**
     * @return 直接依赖列表
     */
    public List<Artifact> getDirectDependencies() {
        List<Artifact> dependencies = new ArrayList<>();
        for (BuildFile child : this.children) {
            dependencies.add(child.getArtifact());
        }
        return dependencies;
    }

    /**
     * @return 所有依赖列表
     */
    public List<Artifact> getDependencies() {
        ArrayList<Artifact> artifacts = new ArrayList<>();
        mergeDependencies(this, artifacts);
        return artifacts;
    }

    /**
     * 合并children的依赖
     */
    private void mergeDependencies(BuildFile buildFile, List<Artifact> dependencies) {
        for (BuildFile dependency : buildFile.children) {
            dependencies.add(dependency.getArtifact());
            mergeDependencies(dependency, dependencies);
        }
    }

    public List<BuildFile> getModules() {
        return modules;
    }

    /**
     * 增加一个子模块
     */
    public void addModule(BuildFile module) {
        this.modules.add(module);
        module.setRoot(this);
    }

    public void setRoot(BuildFile root) {
        this.root = root;
    }

    public BuildFile getRoot() {
        return this.root;
    }

    public BuildFile getChild(String key){
        if(key!=null){
            for (BuildFile child : this.children) {
                if(child.getArtifact().getKey().equals(key)){
                    return child;
                }
            }
        }
        return null;
    }
}
