package se.kth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Getter
@lombok.Setter
public class Dependency {


    public final String groupId;
    public final String artifactId;
    public String version;
    public final String scope;
    public final String type;



    public Dependency(String groupId, String artifactId, String version, String type, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
        this.type = type;
    }

    public Dependency(){
        this.groupId = "";
        this.artifactId = "";
        this.version = "";
        this.scope = "";
        this.type = "";
    }

    @Override
    public String toString() {
        return "Dependency{" + "groupId='" + groupId + '\'' + ", artifactId='" + artifactId + '\'' + ", version='" + version + '\'' + '}';
    }

    @Override
    public int hashCode() {
        return groupId.hashCode() + artifactId.hashCode() + version.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Dependency other = (Dependency) obj;
        return groupId.equals(other.groupId) && artifactId.equals(other.artifactId) && version.equals(other.version);
    }
}
