package com.google.cloud.resourcemanager.spi.v1beta1;

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

  /**
   * Creates a new project.
   *
   * @throws ResourceManagerException upon failure
   */
  // Placeholder method, as Project class is not available
  void createPlaceholder();

  /**
   * Marks the project identified by the specified project ID for deletion.
   *
   * @throws ResourceManagerException upon failure
   */
  void delete(String projectId);

  /**
   * Retrieves the project identified by the specified project ID. Returns {@code null} if the
   * project is not found or if the user doesn't have read permissions for the project.
   *
   * @throws ResourceManagerException upon failure
   */
  // Placeholder method, as Project class is not available
  Object getPlaceholder(String projectId, Map<Option, ?> options);

  /**
   * Lists the projects visible to the current user.
   *
   * @throws ResourceManagerException upon failure
   */
  Tuple<String, Iterable<Object>> list(Map<Option, ?> options);

  /**
   * Restores the project identified by the specified project ID. Undelete will only succeed if the
   * project has a lifecycle state of {@code DELETE_REQUESTED} state. The caller must have modify
   * permissions for this project.
   *
   * @throws ResourceManagerException upon failure
   */
  void undelete(String projectId);

  /**
   * Replaces the attributes of the project. The caller must have modify permissions for this
   * project.
   *
   * @throws ResourceManagerException upon failure
   */
  // Placeholder method, as Project class is not available
  void replacePlaceholder();

  /**
   * Returns the IAM policy associated with a project.
   *
   * @throws ResourceManagerException upon failure
   */
  // Placeholder method, as Policy class is not available
  Object getPolicyPlaceholder(String projectId);

  /**
   * Replaces the IAM policy associated with the given project.
   *
   * @throws ResourceManagerException upon failure
   */
  // Placeholder method, as Policy class is not available
  void replacePolicyPlaceholder(String projectId, Object newPolicy);

  /**
   * Tests whether the caller has the given permissions. Returns a list of booleans corresponding to
   * whether or not the user has the permission in the same position of input list.
   *
   * @throws ResourceManagerException upon failure
   */
  List<Boolean> testPermissions(String projectId, List<String> permissions);

  /**
   * Tests whether the caller has the given permissions on the specified Organization. Returns the
   * permissions and their results.
   *
   * @throws ResourceManagerException upon failure
   */
  Map<String, Boolean> testOrgPermissions(String resource, List<String> permissions) throws IOException;

  // Placeholder methods for OrgPolicy and Constraint, as they are not available
  void clearOrgPolicyPlaceholder(String resource, Object orgPolicy) throws IOException;

  Object getEffectiveOrgPolicyPlaceholder(String resource, String constraint) throws IOException;

  Object getOrgPolicyPlaceholder(String resource, String constraint) throws IOException;

  ListResult<Object> listAvailableOrgPolicyConstraintsPlaceholder(String resource, Map<Option, ?> options) throws IOException;

  ListResult<Object> listOrgPoliciesPlaceholder(String resource, Map<Option, ?> options) throws IOException;

  void replaceOrgPolicyPlaceholder(String resource, Object orgPolicy) throws IOException;
}