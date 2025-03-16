package com.google.cloud.resourcemanager;

import com.google.api.services.cloudresourcemanager.v3.model.BooleanPolicy as BooleanPolicyV3;
import com.google.api.services.cloudresourcemanager.v3.model.ListPolicy as ListPolicyV3;
import com.google.api.services.cloudresourcemanager.v3.model.OrgPolicy as OrgPolicyV3;
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

  static final Function<OrgPolicyV3, OrgPolicyInfo> FROM_PROTOBUF_FUNCTION =
      new Function<OrgPolicyV3, OrgPolicyInfo>() {
        @Override
        public OrgPolicyInfo apply(OrgPolicyV3 protobuf) {
          return OrgPolicyInfo.fromProtobuf(protobuf);
        }
      };
  static final Function<OrgPolicyInfo, OrgPolicyV3> TO_PROTOBUF_FUNCTION =
      new Function<OrgPolicyInfo, OrgPolicyV3>() {
        @Override
        public OrgPolicyV3 apply(OrgPolicyInfo orgPolicyInfo) {
          return orgPolicyInfo.toProtobuf();
        }
      };

  private BoolPolicy boolPolicy;
  private String constraint;
  private String etag;
  private Policies policies;
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

    BooleanPolicyV3 toProtobuf() {
      return new BooleanPolicyV3().setEnforced(enforce);
    }

    static BoolPolicy fromProtobuf(BooleanPolicyV3 booleanPolicy) {
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
    private final String suggestedValue;

    Policies(
        String allValues,
        List<String> allowedValues,
        List<String> deniedValues,
        String suggestedValue) {
      this.allValues = allValues;
      this.allowedValues = allowedValues;
      this.deniedValues = deniedValues;
      this.suggestedValue = suggestedValue;
    }

    /** Returns all the Values state of this policy. */
    String getAllValues() {
      return allValues;
    }

    /** Returns the list of allowed values of this resource */
    List<String> getAllowedValues() {
      return allowedValues;
    }

    /** Returns the list of denied values of this resource. */
    List<String> getDeniedValues() {
      return deniedValues;
    }

    /** Returns the suggested value of this policy. */
    String getSuggestedValue() {
      return suggestedValue;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("allValues", getAllValues())
          .add("allowedValues", getAllowedValues())
          .add("deniedValues", getDeniedValues())
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
          && Objects.equals(suggestedValue, policies.suggestedValue);
    }

    @Override
    public int hashCode() {
      return Objects.hash(allValues, allowedValues, deniedValues, suggestedValue);
    }

    ListPolicyV3 toProtobuf() {
      return new ListPolicyV3()
          .setAllValues(allValues)
          .setAllowedValues(allowedValues)
          .setDeniedValues(deniedValues)
          .setSuggestedValue(suggestedValue);
    }

    static Policies fromProtobuf(ListPolicyV3 listPolicy) {
      return new Policies(
          (listPolicy.getAllValues(),
          listPolicy.getAllowedValues(),
          listPolicy.getDeniedValues(),
          listPolicy.getSuggestedValue());
    }
  }

  /** Builder for {@code OrganizationPolicyInfo}. */
  static class Builder {
    private BoolPolicy boolPolicy;
    private String constraint;
    private String etag;
    private Policies policies;
    private String updateTime;
    private Integer version;

    Builder() {}

    Builder(OrgPolicyInfo info) {
      this.boolPolicy = info.boolPolicy;
      this.constraint = info.constraint;
      this.etag = info.etag;
      this.policies = info.policies;
      this.updateTime = info.updateTime;
      this.version = info.version;
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
        && Objects.equals(updateTime, policyInfo.updateTime)
        && Objects.equals(version, policyInfo.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(boolPolicy, constraint, etag, policies, updateTime, version);
  }

  /** Returns a builder for the {@link OrgPolicyInfo} object. */
  public static Builder newBuilder() {
    return new Builder();
  }

  /** Returns a builder for the {@link OrgPolicyInfo} object. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  OrgPolicyV3 toProtobuf() {
    OrgPolicyV3 orgPolicyProto = new OrgPolicyV3();
    if (boolPolicy != null) {
      orgPolicyProto.setBooleanPolicy(boolPolicy.toProtobuf());
    }
    if (policies != null) {
      orgPolicyProto.setListPolicy(policies.toProtobuf());
    }
    orgPolicyProto.setUpdateTime(updateTime);
    orgPolicyProto.setVersion(version);
    return orgPolicyProto;
  }

  static OrgPolicyInfo fromProtobuf(OrgPolicyV3 orgPolicyProtobuf) {
    Builder builder = newBuilder();
    if (orgPolicyProtobuf.getBooleanPolicy() != null) {
      builder.setBoolPolicy(BoolPolicy.fromProtobuf(orgPolicyProtobuf.getBooleanPolicy()));
    }
    if (orgPolicyProtobuf.getListPolicy() != null) {
      builder.setListPolicy(Policies.fromProtobuf(orgPolicyProtobuf.getListPolicy()));
    }
    builder.setUpdateTime(orgPolicyProtobuf.getUpdateTime());
    builder.setVersion(orgPolicyProtobuf.getVersion());
    return builder.build();
  }
}