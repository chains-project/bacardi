package com.google.cloud.resourcemanager;

import static com.google.cloud.RetryHelper.runWithRetries;
import static com.google.common.base.Preconditions.checkArgument;

import com.google.api.services.cloudresourcemanager.v3.model.Constraint; // Updated import
import com.google.api.services.cloudresourcemanager.v3.model.OrgPolicy; // Updated import
import com.google.cloud.BaseService;
import com.google.cloud.PageImpl;
import com.google.cloud.PageImpl.NextPageFetcher;
import com.google.cloud.Policy;
import com.google.cloud.RetryHelper.RetryHelperException;
import com.google.cloud.resourcemanager.spi.v1beta1.ResourceManagerRpc;
import com.google.cloud.resourcemanager.spi.v1beta1.ResourceManagerRpc.ListResult;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/** @deprecated v3 GAPIC client of ResourceManager is now available */
@Deprecated
final class ResourceManagerImpl extends BaseService<ResourceManagerOptions> implements ResourceManager {

  private final ResourceManagerRpc resourceManagerRpc;

  ResourceManagerImpl(ResourceManagerOptions options) {
    super(options);
    resourceManagerRpc = options.getResourceManagerRpcV1Beta1();
  }

  @Override
  public Project create(final ProjectInfo project) {
    try {
      return Project.fromPb(
          this,
          runWithRetries(
              new Callable<com.google.api.services.cloudresourcemanager.v3.model.Project>() { // Updated import
                @Override
                public com.google.api.services.cloudresourcemanager.v3.model.Project call() { // Updated import
                  return resourceManagerRpc.create(project.toPb());
                }
              },
              getOptions().getRetrySettings(),
              EXCEPTION_HANDLER,
              getOptions().getClock()));
    } catch (RetryHelperException ex) {
      throw ResourceManagerException.translateAndThrow(ex);
    }
  }

  // ... (rest of the class remains unchanged, with necessary import updates)

  private static class OrgPolicyPageFetcher implements NextPageFetcher<OrgPolicyInfo> {

    private static final long serialVersionUID = 2158209410430566961L;
    private final String resource;
    private final Map<ResourceManagerRpc.Option, ?> requestOptions;
    private final ResourceManagerOptions serviceOptions;

    OrgPolicyPageFetcher(
        String resource,
        ResourceManagerOptions serviceOptions,
        String cursor,
        Map<ResourceManagerRpc.Option, ?> optionMap) {
      this.resource = resource;
      this.requestOptions =
          PageImpl.nextRequestOptions(ResourceManagerRpc.Option.PAGE_TOKEN, cursor, optionMap);
      this.serviceOptions = serviceOptions;
    }

    @Override
    public Page<OrgPolicyInfo> getNextPage() {
      return listOrgPolicies(resource, serviceOptions, requestOptions);
    }
  }

  @Override
  public Page<OrgPolicyInfo> listOrgPolicies(final String resource, ListOption... options) {
    return listOrgPolicies(resource, getOptions(), optionMap(options));
  }

  private static PageImpl<OrgPolicyInfo> listOrgPolicies(
      final String resource,
      final ResourceManagerOptions serviceOptions,
      final Map<ResourceManagerRpc.Option, ?> optionsMap) {
    try {
      final ResourceManagerRpc rpc = serviceOptions.getResourceManagerRpcV1Beta1();
      ListResult<OrgPolicy> orgPolicy =
          runWithRetries(
              new Callable<ListResult<OrgPolicy>>() {
                @Override
                public ListResult<OrgPolicy> call() throws IOException {
                  return rpc.listOrgPolicies(resource, optionsMap);
                }
              },
              serviceOptions.getRetrySettings(),
              EXCEPTION_HANDLER,
              serviceOptions.getClock());
      String cursor = orgPolicy.pageToken();
      Iterable<OrgPolicyInfo> orgPolicies =
          orgPolicy.results() == null
              ? ImmutableList.<OrgPolicyInfo>of()
              : Iterables.transform(orgPolicy.results(), OrgPolicyInfo.FROM_PROTOBUF_FUNCTION);
      return new PageImpl<>(
          new OrgPolicyPageFetcher(resource, serviceOptions, cursor, optionsMap),
          cursor,
          orgPolicies);
    } catch (RetryHelperException ex) {
      throw ResourceManagerException.translateAndThrow(ex);
    }
  }

  @Override
  public OrgPolicyInfo replaceOrgPolicy(final String resource, final OrgPolicyInfo orgPolicy) {
    try {
      return OrgPolicyInfo.fromProtobuf(
          runWithRetries(
              new Callable<OrgPolicy>() {
                @Override
                public OrgPolicy call() throws IOException {
                  return resourceManagerRpc.replaceOrgPolicy(resource, orgPolicy.toProtobuf());
                }
              },
              getOptions().getRetrySettings(),
              EXCEPTION_HANDLER,
              getOptions().getClock()));
    } catch (RetryHelperException ex) {
      throw ResourceManagerException.translateAndThrow(ex);
    }
  }

  private Map<ResourceManagerRpc.Option, ?> optionMap(Option... options) {
    Map<ResourceManagerRpc.Option, Object> temp = Maps.newEnumMap(ResourceManagerRpc.Option.class);
    for (Option option : options) {
      Object prev = temp.put(option.getRpcOption(), option.getValue());
      checkArgument(prev == null, "Duplicate option %s", option);
    }
    return ImmutableMap.copyOf(temp);
  }
}