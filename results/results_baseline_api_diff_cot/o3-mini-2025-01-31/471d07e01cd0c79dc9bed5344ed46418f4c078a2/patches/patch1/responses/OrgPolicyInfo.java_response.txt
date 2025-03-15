/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.resourcemanager;

import com.google.api.services.cloudresourcemanager.v3.model.Policy;
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

  static final Function<Policy, OrgPolicyInfo> FROM_PROTOBUF_FUNCTION =
      new Function<Policy, OrgPolicyInfo>() {
        @Override
        public OrgPolicyInfo apply(Policy policyProtobuf) {
          return OrgPolicyInfo.fromProtobuf(policyProtobuf);
        }
      };
  static final Function<OrgPolicyInfo, Policy> TO_PROTOBUF_FUNCTION =
      new Function<OrgPolicyInfo, Policy>() {
        @Override
        public Policy apply(OrgPolicyInfo orgPolicyInfo) {
          return orgPolicyInfo.toProtobuf();
        }
      };

  private BoolPolicy boolPolicy;
  private String constraint;
  private String etag;
  private Policies policies;
  private Policy.RestoreDefault restoreDefault;
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

    Policy.BooleanPolicy toProtobuf() {
      // Create a new instance of the v3 BooleanPolicy and set the enforcement flag.
      return new Policy.BooleanPolicy().setEnforced(enforce);
    }

    static BoolPolicy fromProtobuf(Policy.BooleanPolicy booleanPolicy) {
      return new BoolPolicy(booleanPolicy.getEnforced());
    }
  }

  /**
   * The organization ListPolicy object.
   *
   * <p>ListPolicy can define specific values and subtrees of Cloud Resource Manager resource
   * hierarchy (Organizations, Folders, Projects) that are allowed or denied. In the previous API,
   * fields like allowedValues, deniedValues, allValues, inheritFromParent, and suggestedValue were
   * configurable. In the new v3 API these setters and getters have been removed. As a result, the
   * conversion routines here no longer propagate those values.
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

    /** Returns the inheritance behavior for this Policy */
    Boolean getInheritFromParent() {
      return inheritFromParent;
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

    Policy.ListPolicy toProtobuf() {
      // The new v3 Policy.ListPolicy does not support the previous setters for allValues,
      // allowedValues, deniedValues, inheritFromParent, and suggestedValue.
      // Therefore, we simply create a new instance without setting these values.
      return new Policy.ListPolicy();
    }

    static Policies fromProtobuf(Policy.ListPolicy listPolicy) {
      // The getters for these values are no longer supported in the new v3 API.
      // We return a Policies instance with all fields set to null.
      return new Policies(null, null, null, null, null);
    }
  }

  /** Builder for {@code OrgPolicyInfo}. */
  static class Builder {
    private BoolPolicy boolPolicy;
    private String constraint;
    private String etag;
    private Policies policies;
    private Policy.RestoreDefault restoreDefault;
    private String updateTime;
    private Integer version;

    Builder() {}

    Builder(OrgPolicyInfo info) {
      this.boolPolicy = info.boolPolicy;
      this.constraint = info.constraint;
      this.etag = info.etag;
      this.policies = info.policies;
      this.restoreDefault = info.restoreDefault;
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

    Builder setRestoreDefault(Policy.RestoreDefault restoreDefault) {
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
  public Policy.RestoreDefault getRestoreDefault() {
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
    return Objects.hash(boolPolicy, constraint, etag, policies, restoreDefault, updateTime, version);
  }

  /** Returns a builder for the {@link OrgPolicyInfo} object. */
  public static Builder newBuilder() {
    return new Builder();
  }

  /** Returns a builder for the {@link OrgPolicyInfo} object. */
  public Builder toBuilder() {
    return new Builder(this);
  }

  Policy toProtobuf() {
    Policy orgPolicyProto = new Policy();
    if (boolPolicy != null) {
      orgPolicyProto.setBooleanPolicy(boolPolicy.toProtobuf());
    }
    // The constraint field is no longer set in the new v3 API.
    if (policies != null) {
      orgPolicyProto.setListPolicy(policies.toProtobuf());
    }
    orgPolicyProto.setRestoreDefault(restoreDefault);
    orgPolicyProto.setEtag(etag);
    // updateTime is not set because the setter has been removed.
    orgPolicyProto.setVersion(version);
    return orgPolicyProto;
  }

  static OrgPolicyInfo fromProtobuf(Policy orgPolicyProtobuf) {
    Builder builder = newBuilder();
    if (orgPolicyProtobuf.getBooleanPolicy() != null) {
      builder.setBoolPolicy(BoolPolicy.fromProtobuf(orgPolicyProtobuf.getBooleanPolicy()));
    }
    // The constraint field is not available in the new v3 API.
    if (orgPolicyProtobuf.getListPolicy() != null) {
      builder.setListPolicy(Policies.fromProtobuf(orgPolicyProtobuf.getListPolicy()));
    }
    builder.setRestoreDefault(orgPolicyProtobuf.getRestoreDefault());
    builder.setEtag(orgPolicyProtobuf.getEtag());
    // updateTime is not available in the new v3 API.
    builder.setVersion(orgPolicyProtobuf.getVersion());
    return builder.build();
  }
}