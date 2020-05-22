package com.huawei.fossbot.dependency.bean;

import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 项目唯一标识
 */
public class Artifact implements Comparable<Artifact> {
    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private String scope;
    private boolean optional = false;
    private Set<String> excludes = new HashSet<>();
    private String source;
    private String md5;

    @Override
    public int compareTo(Artifact o) {
        return this.getKey().compareTo(o.getKey());
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + (version == null ? ":" : ":" + version);
    }

    public String getKey() {
        return groupId + ":" + artifactId;
    }

    Artifact copy() {
        Artifact artifact = new Artifact();
        artifact.groupId = groupId;
        artifact.artifactId = artifactId;
        artifact.version = version;
        artifact.classifier = classifier;
        artifact.optional = optional;
        artifact.md5 = md5;
        return artifact;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Artifact)) return false;
        Artifact artifact = (Artifact) obj;
        return StringUtils.equals(groupId, artifact.groupId)
                && StringUtils.equals(artifactId, artifact.artifactId)
                && StringUtils.equals(version, artifact.version)
                && StringUtils.equals(classifier, artifact.classifier)
                && StringUtils.equals(md5,artifact.getMd5());
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (groupId != null) hash += groupId.hashCode();
        if (artifactId != null) hash += artifactId.hashCode();
        if (version != null) hash += version.hashCode();
        if (classifier != null) hash += classifier.hashCode();
        return hash;
    }

    public void setSource(Artifact artifact) {
        source = artifact.toString();
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public Set<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(Set<String> excludes) {
        this.excludes = excludes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMd5(){
        return this.md5;
    }

    public void setMd5(String md5){
        this.md5 = md5;
    }
}