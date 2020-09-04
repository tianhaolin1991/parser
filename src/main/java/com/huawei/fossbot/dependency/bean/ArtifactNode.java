package com.huawei.fossbot.dependency.bean;

import java.util.Objects;

public class ArtifactNode {
    private Artifact artifact = null;
    private String parentCoordinate = null;

    public ArtifactNode parentCoordinate(String parentCoordinate) {
        this.parentCoordinate = parentCoordinate;
        return this;
    }

    public ArtifactNode artifact(Artifact artifact){
        this.artifact = artifact;
        return this;
    }

    public String getMd5(){
        return this.artifact.getMd5();
    }

    public String getScope(){
        return this.artifact.getScope();
    }

    public String getGroupId(){
        return this.artifact.getGroupId();
    }

    public String getArtifactId(){
        return this.artifact.getArtifactId();
    }

    public String getVersion(){
        return this.artifact.getVersion();
    }

    public void setScope(String scope){
        this.artifact.setScope(scope);
    }

    public String getKey(){
        return this.artifact.toString();
    }

    public String getParentCoordinate() {
        return parentCoordinate;
    }

    public void setParentCoordinate(String parentCoordinate) {
        this.parentCoordinate = parentCoordinate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArtifactNode fileInfo = (ArtifactNode) o;
        return Objects.equals(this.artifact, fileInfo.artifact) &&
                Objects.equals(this.parentCoordinate, fileInfo.parentCoordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact,parentCoordinate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ArtifactNode {\n");
        sb.append("    artifact: ").append(toIndentedString(artifact)).append("\n");
        sb.append("    parentCoordinate: ").append(toIndentedString(parentCoordinate)).append("\n");
        sb.append("}");
        return sb.toString();
    }


    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

