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
   * hierarchy (Organizations, Folders, Projects) that are allowed or denied.
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
      return Objects.hash(
          allValues, allowedValues, deniedValues, inheritFromParent, suggestedValue);
    }

    ListPolicy toProtobuf() {
      return new ListPolicy()
          .setAllValues(allValues)
          .setAllowedValues(allowedValues)
          .setDeniedValues(deniedValues)
          .setInheritFromParent(inheritFromParent)
          .setSuggestedValue(suggestedValue);
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

  /** Returns a builder for the {@link OrgPolicyInfo} object. */
  public static Builder newBuilder() {
    return new Builder();
  }

  /** Returns a builder for the {@link OrgPolicyInfo} object. */
  public Builder toBuilder() {
    return new Builder(this);
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

  public static class OrgPolicy extends Policy {
    private BooleanPolicy booleanPolicy;
    private ListPolicy listPolicy;
    private RestoreDefault restoreDefault;

    public OrgPolicy() {
      super();
    }

    public OrgPolicy setBooleanPolicy(BooleanPolicy bp) {
      this.booleanPolicy = bp;
      return this;
    }

    public BooleanPolicy getBooleanPolicy() {
      return this.booleanPolicy;
    }

    public OrgPolicy setListPolicy(ListPolicy lp) {
      this.listPolicy = lp;
      return this;
    }

    public ListPolicy getListPolicy() {
      return this.listPolicy;
    }

    public OrgPolicy setRestoreDefault(RestoreDefault rd) {
      this.restoreDefault = rd;
      return this;
    }

    public RestoreDefault getRestoreDefault() {
      return this.restoreDefault;
    }

    public OrgPolicy setConstraint(String constraint) {
      this.set("constraint", constraint);
      return this;
    }

    public String getConstraint() {
      Object value = this.get("constraint");
      return value == null ? null : value.toString();
    }
  }

  public static class BooleanPolicy {
    private Boolean enforced;

    public BooleanPolicy setEnforced(Boolean enforced) {
      this.enforced = enforced;
      return this;
    }

    public Boolean getEnforced() {
      return enforced;
    }
  }

  public static class ListPolicy {
    private String allValues;
    private List<String> allowedValues;
    private List<String> deniedValues;
    private Boolean inheritFromParent;
    private String suggestedValue;

    public ListPolicy setAllValues(String allValues) {
      this.allValues = allValues;
      return this;
    }

    public ListPolicy setAllowedValues(List<String> allowedValues) {
      this.allowedValues = allowedValues;
      return this;
    }

    public ListPolicy setDeniedValues(List<String> deniedValues) {
      this.deniedValues = deniedValues;
      return this;
    }

    public ListPolicy setInheritFromParent(Boolean inheritFromParent) {
      this.inheritFromParent = inheritFromParent;
      return this;
    }

    public ListPolicy setSuggestedValue(String suggestedValue) {
      this.suggestedValue = suggestedValue;
      return this;
    }

    public String getAllValues() {
      return allValues;
    }

    public List<String> getAllowedValues() {
      return allowedValues;
    }

    public List<String> getDeniedValues() {
      return deniedValues;
    }

    public Boolean getInheritFromParent() {
      return inheritFromParent;
    }

    public String getSuggestedValue() {
      return suggestedValue;
    }
  }

  public static class RestoreDefault {
  }
}