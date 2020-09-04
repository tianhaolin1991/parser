package com.huawei.fossbot.dependency.dsl.gradle;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import static com.huawei.fossbot.dependency.util.FileUtils.getUniformPathStr;

/**
 * bean to describe a project
 *
 * @author t30002128
 * @since 2020/06/03
 */
public  class ProjectDescriptor extends GroovyObjectSupport {
    private static final String BUILD_SCRIPT_BASENAME = "build.gradle";
    private String buildFilePath;
    private String name;
    private String dir;
    private ProjectDescriptor parent;
    private List<ProjectDescriptor> children = new ArrayList<>();
    private Path path;
    private String buildFileName = BUILD_SCRIPT_BASENAME;
    private Boolean isAndroid = false;
    private RepositoryHandler repositories = new RepositoryHandler();
    private String libDir;

    public ProjectDescriptor(File dir) {
        this.dir = getUniformPathStr(dir);
        this.name = dir.getName();
        this.buildFilePath = getUniformPathStr
                (Paths.get(this.dir).resolve(this.buildFileName));
    }

    public ProjectDescriptor(ProjectDescriptor parent, String name, File dir) {
        this.parent = parent;
        this.dir = getUniformPathStr(dir);
        this.name = name;
        this.buildFilePath = getUniformPathStr
                (Paths.get(this.dir).resolve(this.buildFileName));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Path path(String name) {
        if (isRootDescriptor()) {
            return path = path.ROOT;
        } else {
            return parent.absolutePath(name);
        }
    }

    private Path absolutePath(String path) {
        return this.path.child(path);
    }

    private boolean isRootDescriptor() {
        return this.parent == null;
    }

    public String getBuildFileName() {
        return buildFileName;
    }

    public void setBuildFileName(String buildFileName) {
        this.buildFileName = buildFileName;
    }

    public void setParent(ProjectDescriptor parent) {
        this.parent = parent;
    }

    public List<ProjectDescriptor> getChildren() {
        return children;
    }

    public void setChildren(List<ProjectDescriptor> children) {
        this.children = children;
    }

    /**
     * @return dir
     */
    public String getProjectDir() {
        return dir;
    }

    /**
     * @param dir 目录
     */
    public void setProjectDir(String dir) {
        this.dir = dir;
    }


    @Override
    public void setProperty(String propertyName, Object newValue){
        if(getMetaClass().getMetaProperty(propertyName)!=null){
            getMetaClass().setProperty(this, propertyName, newValue);
        }
    }

    @Override
    public Object getProperty(String propertyName) {
        try {
            return super.getProperty(propertyName);
        } catch (MissingPropertyException e) {
            return null;
        }
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        if(getMetaClass().getMetaMethod(name,new Object[]{args})!=null){
            return getMetaClass().invokeMethod(this,name,args);
        }else{
            return null;
        }
    }

    public void isAndroid(Boolean isAndroid) {
        this.isAndroid = isAndroid;
    }
    public Boolean isAndroid() {
        return this.isAndroid;
    }

    public void setLibDir(String relativeLibDir) {
        this.libDir = getUniformPathStr(Paths.get(this.dir).resolve(relativeLibDir));
    }
    public String getLibDir() {
        return libDir;
    }

    public RepositoryHandler getRepo() {
        return this.repositories;
    }

    public String getBuildFilePath() {
        return this.buildFilePath;
    }
}
