package se.kth.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import se.kth.models.FailureCategory;

public class BreakingUpdate {
    public final String url;
    public final String project;
    public final String projectOrganisation;
    public final String breakingCommit;
    public final String prAuthor;
    public final String preCommitAuthor;
    public final String breakingCommitAuthor;
    public final UpdatedDependency updatedDependency;
    public final String preCommitReproductionCommand;
    public final String breakingUpdateReproductionCommand;
    public final String javaVersionUsedForReproduction;
    public final FailureCategory failureCategory;
    public final String licenseInfo;

    @JsonCreator
    BreakingUpdate(@JsonProperty("url") String url,
                   @JsonProperty("project") String project,
                   @JsonProperty("projectOrganisation") String organisation,
                   @JsonProperty("breakingCommit") String breakingCommit,
                   @JsonProperty("prAuthor") String prAuthor,
                   @JsonProperty("preCommitAuthor") String preCommitAuthor,
                   @JsonProperty("breakingCommitAuthor") String breakingCommitAuthor,
                   @JsonProperty("updatedDependency") UpdatedDependency updatedDependency,
                   @JsonProperty("preCommitReproductionCommand") String preCommitReproductionCommand,
                   @JsonProperty("breakingUpdateReproductionCommand") String breakingUpdateReproductionCommand,
                   @JsonProperty("javaVersionUsedForReproduction") String javaVersionUsedForReproduction,
                   @JsonProperty("failureCategory") FailureCategory failureCategory,
                   @JsonProperty("licenseInfo") String licenseInfo) {
        this.url = url;
        this.project = project;
        this.projectOrganisation = organisation;
        this.breakingCommit = breakingCommit;
        this.prAuthor = prAuthor;
        this.preCommitAuthor = preCommitAuthor;
        this.breakingCommitAuthor = breakingCommitAuthor;
        this.updatedDependency = updatedDependency;
        this.preCommitReproductionCommand = preCommitReproductionCommand;
        this.breakingUpdateReproductionCommand = breakingUpdateReproductionCommand;
        this.javaVersionUsedForReproduction = javaVersionUsedForReproduction;
        this.failureCategory = failureCategory;
        this.licenseInfo = licenseInfo;
    }

    public String getPreImageId() {
        return this.preCommitReproductionCommand.replace("docker run ", "");
    }

    public String getBreakingImageId() {
        return this.breakingUpdateReproductionCommand.replace("docker run ", "");
    }
}
