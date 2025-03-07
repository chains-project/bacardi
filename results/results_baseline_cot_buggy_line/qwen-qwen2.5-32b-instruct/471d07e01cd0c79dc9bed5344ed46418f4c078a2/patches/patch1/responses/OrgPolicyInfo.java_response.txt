package com.google.cloud.resourcemanager;

import com.google.cloud.resourcemanager.v3.OrgPolicy; // Assuming the new package is v3
import com.google.cloud.resourcemanager.v3.RestoreDefault; // Assuming the new package is v3
import com.google.cloud.resourcemanager.v3.BooleanPolicy; // Assuming the new package is v3
import com.google.cloud.resourcemanager.v3.ListPolicy; // Assuming the new package is v3
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;

/**
 * A Google Cloud Resource Manager organization policy metadata object.
 *
 * <p>Defines a Cloud Organization Policy which specifies constraints for configurations of Cloud
 * Platform resources.
 *
 * @deprecated v3 GAPIC client of ResourceManager is now available
 */
@Deprecated
public class OrgPolicyInfo {

  static final Function<OrgPolicy, OrgPolicyInfo> FROM_PROTOBUF_FUNCTION =
      new Function<OrgPolicy, OrgPolicyInfo>() {
        @Override
        public OrgPolicyInfo apply(OrgPolicy protobuf) {
          return OrgPolicyInfo.fromProtobuf(protobuf);
        }
      };
  static final Function<OrgPolicyInfo, OrgPolicy> TO_PROTOBUF_FUNCTION =
      new Function<OrgPolicyInfo, OrgPolicy>() {
        @Override
        public OrgPolicy apply(OrgPolicyInfo orgPolicyInfo) {
          return orgPolicyInfo.toProtobuf();
        }
      };

  private BoolPolicy boolPolicy;
  private String constraint;
  private String etag;
  private Policies policies;
  private RestoreDefault restoreDefault;
  private String updateTime;
  private Integer version;

  /** Used For boolean Constraints, whether to enforce the Constraint or not. */
  static class BoolPolicy {

    private final Boolean enforce;

    BoolPolicy(Boolean enforce) {
      this.enforce = enforce;
    }

    public boolean getEnforce() {
      return enforce;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("enforce", getEnforce()).toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      BoolPolicy that = (BoolPolicy) o;
      return Objects.equals(enforce, that.enforce);
    }

    @Override
    public int hashCode() {
      return Objects.hash(enforce);
    }

    BooleanPolicy toProtobuf() {
      return new BooleanPolicy().setEnforced(enforce);
    }

    static BoolPolicy fromProtobuf(BooleanPolicy booleanPolicy) {
      return new BoolPolicy(booleanPolicy.getEnforced());
    }
  }

  /**
   * The organization ListPolicy object.
   *
   * <p>ListPolicy can define specific values and subtrees of Cloud Resource Manager resource
   * hierarchy (Organizations, Folders, Projects) that are allowed or denied by setting the
   * allowedValues and deniedValues fields.
   */
  static class Policies {

    private final String allValues;
    private final List<String> allowedValues;
    private final List<String> deniedValues;
    private final Boolean inheritFromParent;
    private final String suggestedValue;

    Policies(
        String allValues,
        List<String> allowedValues,
        List<String> deniedValues,
        Boolean inheritFromParent,
        String suggestedValue) {
      this.allValues = allValues;
      this.allowedValues = allowedValues;
      this.deniedValues = deniedValues;
      this.inheritFromParent = inheritFromParent;
      this.suggestedValue = suggestedValue;
    }

    String getAllValues() {
      return allValues;
    }

    List<String> getAllowedValues() {
      return allowedValues;
    }

    List<String> getDeniedValues() {
      return deniedValues;
    }

    Boolean getInheritFromParent() {
      return inheritFromParent;
    }

    String getSuggestedValue() {
      return suggestedValue;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("allValues", getAllValues())
          .add("allowedValues", getAllowedValues())
          .add("deniedValues", getDeniedValues())
          .add("inheritFromParent", getInheritFromParent())
          .add("suggestedValue", getSuggestedValue())
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Policies policies = (Policies) o;
      return Objects.equals(allValues, policies.allValues)
          && Objects.equals(allowedValues, policies.allowedValues)
          && Objects.equals(deniedValues, policies.deniedValues)
          && Objects.equals(inheritFromParent, policies.inheritFromParent)
          && Objects.equals(suggestedValue, policies.suggestedValue);
    }

    @Override
    public int hashCode() {
      return Objects.hash(allValues, allowedValues, deniedValues, inheritFromParent, suggestedValue);
    }

    ListPolicy toProtobuf() {
      return new ListPolicy()
          .setAllValues(getAllValues())
          .setAllowedValues(getAllowedValues())
          .setDeniedValues(getDeniedValues())
          .setInheritFromParent(getInheritFromParent())
          .setSuggestedValue(getSuggestedValue());
    }

    static Policies fromProtobuf(ListPolicy listPolicy) {
      return new Policies(
          listPolicy.getAllValues(),
          listPolicy.getAllowedValues(),
          listPolicy.getDeniedValues(),
          listPolicy.getInheritFromParent(),
          listPolicy.getSuggestedValue());
    }
  }

  /** Builder for {@code OrganizationPolicyInfo}. */
  static class Builder {
    private BoolPolicy boolPolicy;
    private String constraint;
    private String etag;
    private Policies policies;
    private RestoreDefault restoreDefault;
    private String updateTime;
    private Integer version;

    Builder() {}

    Builder(OrgPolicyInfo orgPolicyInfo) {
      this.boolPolicy = orgPolicyInfo.boolPolicy;
      this.constraint = orgPolicyInfo.constraint;
      this.etag = orgPolicyInfo.etag;
      this.policies = orgPolicyInfo.policies;
      this.restoreDefault = orgPolicyInfo.restoreDefault;
      this.updateTime = orgPolicyInfo.updateTime;
      this.version = orgPolicyInfo.version;
    }

    Builder setBoolPolicy(BoolPolicy boolPolicy) {
      this.boolPolicy = boolPolicy;
      return this;
    }

    Builder setConstraint(String constraint) {
      this.constraint = constraint;
      return this;
    }

    Builder setEtag(String etag) {
      this.etag = etag;
      return this;
    }

    Builder setListPolicy(Policies policies) {
      this.policies = policies;
      return this;
    }

    Builder setRestoreDefault(RestoreDefault restoreDefault) {
      this.restoreDefault = restoreDefault;
      return this;
    }

    Builder setUpdateTime(String updateTime) {
      this.updateTime = updateTime;
      return this;
    }

    Builder setVersion(Integer version) {
      this.version = version;
      return this;
    }

    OrgPolicyInfo build() {
      return new OrgPolicyInfo(this);
    }
  }

  OrgPolicyInfo(Builder builder) {
    this.boolPolicy = builder.boolPolicy;
    this.constraint = builder.constraint;
    this.etag = builder.etag;
    this.policies = builder.policies;
    this.restoreDefault = builder.restoreDefault;
    this.updateTime = builder.updateTime;
    this.version = builder.version;
  }

  /** Returns the boolean constraint to check whether the constraint is enforced or not. */
  public BoolPolicy getBoolPolicy() {
    return boolPolicy;
  }

  /** Returns the name of the Constraint. */
  public String getConstraint() {
    return constraint;
  }

  /** Returns the etag value of policy. */
  public String getEtag() {
    return etag;
  }

  /** Return the policies. */
  public Policies getPolicies() {
    return policies;
  }

  /** Restores the default behavior of the constraint. */
  public RestoreDefault getRestoreDefault() {
    return restoreDefault;
  }

  /** Returns the updated timestamp of policy. */
  public String getUpdateTime() {
    return updateTime;
  }

  /** Returns the version of the Policy, Default version is 0. */
  public Integer getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrgPolicyInfo policyInfo = (OrgPolicyInfo) o;
    return Objects.equals(boolPolicy, policyInfo.boolPolicy)
        && Objects.equals(constraint, policyInfo.constraint)
        && Objects.equals(etag, policyInfo.etag)
        && Objects.equals(policies, policyInfo.policies)
        && Objects.equals(restoreDefault, policyInfo.restoreDefault)
        && Objects.equals(updateTime, policyInfo.updateTime)
        && Objects.equals(version, policyInfo.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        boolPolicy, constraint, etag, policies, restoreDefault, updateTime, version);
  }

  OrgPolicy toProtobuf() {
    OrgPolicy orgPolicyProto = new OrgPolicy();
    if (boolPolicy != null) {
      orgPolicyProto.setBooleanPolicy(boolPolicy.toProtobuf());
    }
    orgPolicyProto.setConstraint(constraint);
    if (policies != null) {
      orgPolicyProto.setListPolicy(policies.toProtobuf());
    }
    orgPolicyProto.setRestoreDefault(restoreDefault);
    orgPolicyProto.setEtag(etag);
    orgPolicyProto.setUpdateTime(updateTime);
    orgPolicyProto.setVersion(version);
    return orgPolicyProto;
  }

  static OrgPolicyInfo fromProtobuf(OrgPolicy orgPolicyProtobuf) {
    Builder builder = newBuilder();
    if (orgPolicyProtobuf.getBooleanPolicy() != null) {
      builder.setBoolPolicy(BoolPolicy.fromProtobuf(orgPolicyProtobuf.getBooleanPolicy()));
    }
    builder.setConstraint(orgPolicyProtobuf.getConstraint());
    if (orgPolicyProtobuf.getListPolicy() != null) {
      builder.setListPolicy(Policies.fromProtobuf(orgPolicyProtobuf.getListPolicy()));
    }
    builder.setRestoreDefault(orgPolicyProtobuf.getRestoreDefault());
    builder.setEtag(orgPolicyProtobuf.getEtag());
    builder.setUpdateTime(orgPolicyProtobuf.getUpdateTime());
    builder.setVersion(orgPolicyProtobuf.getVersion());
    return builder.build();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }
}