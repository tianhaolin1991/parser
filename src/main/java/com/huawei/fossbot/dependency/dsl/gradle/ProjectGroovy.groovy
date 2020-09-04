package com.huawei.fossbot.dependency.dsl.gradle

abstract class ProjectGroovy extends Script {

    def project;

    void init(Settings settings) {
        project = new Project(settings);
    }

    void dependencies(Closure closure) {
        project.dependencies(closure)
    }

    void plugins(Closure closure) {
        closure.call()
        project.dependencies.configurations.keySet().each {
            this.metaClass."$it" = {
                Map artifact ->
                    if (artifact) {
                        project.dependencies.add("$it", "${artifact.group}", "${artifact.name}", "${artifact.version}")
                    }
            }
            this.metaClass."$it" = {
                Map artifact, Closure c ->
                    if(artifact){
                        project.dependencies.add("$it", "${artifact.group}", "${artifact.name}", "${artifact.version}", c)
                    }
            }
        }
    }

    void configurations(Closure closure) {

    }

    def id(String id) {
        def classFullName = plugins.get(id)
        if (classFullName) {
            def plugin = Class.forName(classFullName).newInstance()
            project.dependencies.addConfigurations(plugin.getConfigurations())
        }
    }

    void repositories(Closure closure) {
        closure.call()
    }

    void group(String group) {
        project.artifact.groupId = group
    }

    void version(String version) {
        project.artifact.version = version
    }

    void name(String name) {
        project.artifact.artifactId = name;
    }

    void maven(Closure closure) {
        closure.run()
    }

    def mavenCentral() {
        project.repositories.mavenCentral()
    }

    def mavenLocal() {
        project.repositories.mavenLocal()
    }

    def url(String url) {
        project.repositories.addRemoteRepo(url)
    }

    Object getProperty(String propertyName) {
        try {
            return super.getProperty(propertyName)
        } catch (MissingPropertyException e) {
            return null
        }
    }

    Object invokeMethod(String name, Object args) {
        try {
            super.invokeMethod(name, args)
        } catch (MissingMethodException mme) {
            return null
        }
    }
}