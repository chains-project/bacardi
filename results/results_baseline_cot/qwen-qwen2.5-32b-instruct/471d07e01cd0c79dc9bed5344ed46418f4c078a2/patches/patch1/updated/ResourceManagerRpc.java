package com.google.cloud.resourcemanager.spi.v1beta1;

import com.google.cloud.resourcemanager.v3.model.Constraint;
import com.google.cloud.resourcemanager.v3.model.OrgPolicy;
import com.google.cloud.resourcemanager.v3.model.Policy;
import com.google.cloud.resourcemanager.v3.model.Project;
import com.google.cloud.ServiceRpc;
import com.google.cloud.Tuple;
import com.google.cloud.resourcemanager.ResourceManagerException;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** @deprecated v3 GAPIC client of ResourceManager is now available */
@Deprecated
public interface ResourceManagerRpc extends ServiceRpc {

  enum Option {
    FILTER("filter"),
    FIELDS("fields"),
    PAGE_SIZE("pageSize"),
    PAGE_TOKEN("pageToken");

    private final String value;

    Option(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }

    @SuppressWarnings("unchecked")
    <T> T get(Map<Option, ?> options) {
      return (T) options.get(this);
    }

    String getString(Map<Option, ?> options) {
      return get(options);
    }

    Integer getInt(Map<Option, ?> options) {
      return get(options);
    }
  }

  class ListResult<T> {

    private final Iterable<T> results;
    private final String pageToken;

    ListResult(String pageToken, Iterable<T> results) {
      this.results = ImmutableList.copyOf(results);
      this.pageToken = pageToken;
    }

    public Iterable<T> results() {
      return results;
    }

    public String pageToken() {
      return pageToken;
    }
  }

  Project create(Project project);

  void delete(String projectId);

  Project get(String projectId, Map<Option, ?> options);

  Tuple<String, Iterable<Project>> list(Map<Option, ?> options);

  void undelete(String projectId);

  Project replace(Project project);

  Policy getPolicy(String projectId);

  Policy replacePolicy(String projectId, Policy newPolicy);

  List<Boolean> testPermissions(String projectId, List<String> permissions);

  Map<String, Boolean> testOrgPermissions(String resource, List<String> permissions) throws IOException;

  void clearOrgPolicy(String resource, OrgPolicy orgPolicy) throws IOException;

  OrgPolicy getEffectiveOrgPolicy(String resource, String constraint) throws IOException;

  ListResult<Constraint> listAvailableOrgPolicyConstraints(String resource, Map<Option, ?> options) throws IOException;

  ListResult<OrgPolicy> listOrgPolicies(String resource, Map<Option, ?> options) throws IOException;

  OrgPolicy replaceOrgPolicy(String resource, OrgPolicy orgPolicy) throws IOException;
}