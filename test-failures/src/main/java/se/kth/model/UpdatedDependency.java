package se.kth.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdatedDependency {
    public final String dependencyGroupID;
    public final String dependencyArtifactID;
    public final String previousVersion;
    public final String newVersion;
    public final String dependencyScope;
    public final String versionUpdateType;
    public final String githubCompareLink;
    public final String mavenSourceLinkPre;
    public final String mavenSourceLinkBreaking;
    public final String updatedFileType;
    public final String dependencySection;
    public final String licenseInfo;
    public final String githubRepoSlug;

    @JsonCreator
    UpdatedDependency(@JsonProperty("dependencyGroupID") String dependencyGroupID,
                      @JsonProperty("dependencyArtifactID") String dependencyArtifactID,
                      @JsonProperty("previousVersion") String previousVersion,
                      @JsonProperty("newVersion") String newVersion,
                      @JsonProperty("dependencyScope") String dependencyScope,
                      @JsonProperty("versionUpdateType") String versionUpdateType,
                      @JsonProperty("githubCompareLink") String githubCompareLink,
                      @JsonProperty("mavenSourceLinkPre") String mavenSourceLinkPre,
                      @JsonProperty("mavenSourceLinkBreaking") String mavenSourceLinkBreaking,
                      @JsonProperty("updatedFileType") String updatedFileType,
                      @JsonProperty("dependencySection") String dependencySection,
                      @JsonProperty("licenseInfo") String licenseInfo,
                      @JsonProperty("githubRepoSlug") String githubRepoSlug) {
        this.dependencyGroupID = dependencyGroupID;
        this.dependencyArtifactID = dependencyArtifactID;
        this.previousVersion = previousVersion;
        this.newVersion = newVersion;
        this.dependencyScope = dependencyScope;
        this.versionUpdateType = versionUpdateType;
        this.githubCompareLink = githubCompareLink;
        this.mavenSourceLinkPre = mavenSourceLinkPre;
        this.mavenSourceLinkBreaking = mavenSourceLinkBreaking;
        this.updatedFileType = updatedFileType;
        this.dependencySection = dependencySection;
        this.licenseInfo = licenseInfo;
        this.githubRepoSlug = githubRepoSlug;
    }

}
