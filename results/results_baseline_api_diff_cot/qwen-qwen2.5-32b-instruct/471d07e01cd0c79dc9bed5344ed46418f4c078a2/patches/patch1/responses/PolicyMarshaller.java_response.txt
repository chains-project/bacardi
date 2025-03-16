package com.google.cloud.resourcemanager;

import com.google.api.services.cloudresourcemanager.v3.model.Binding;
import com.google.api.services.cloudresourcemanager.v3.model.Policy as V3Policy;
import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Policy.Marshaller;
import com.google.cloud.Role;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @deprecated v3 GAPIC client of ResourceManager is now available */
@Deprecated
final class PolicyMarshaller
    extends Marshaller<com.google.api.services.cloudresourcemanager.v3.model.Policy> {

  static final PolicyMarshaller INSTANCE = new PolicyMarshaller();

  private PolicyMarshaller() {}

  @Override
  protected Policy fromPb(V3Policy policyPb) {
    Map<Role, Set<Identity>> bindings = new HashMap<>();
    if (policyPb.getBindings() != null) {
      for (Binding bindingPb : policyPb.getBindings()) {
        bindings.put(
            Role.of(bindingPb.getRole()),
            ImmutableSet.copyOf(
                Lists.transform(
                    new ArrayList<>(bindingPb.getMembers()),
                    new Function<String, Identity>() {
                      @Override
                      public Identity apply(String s) {
                        return IDENTITY_VALUE_OF_FUNCTION.apply(s);
                      }
                    })));
      }
    }
    return new Builder(bindings, policyPb.getEtag(), policyPb.getVersion()).build();
  }

  @Override
  protected V3Policy toPb(Policy policy) {
    V3Policy policyPb = new V3Policy();
    List<Binding> bindingPbList = new LinkedList<>();
    for (Map.Entry<Role, Set<Identity>> binding : policy.getBindings().entrySet()) {
      Binding bindingPb = new Binding();
      bindingPb.setRole(binding.getKey().getValue());
      bindingPb.setMembers(
          Lists.transform(
              new ArrayList<>(binding.getValue()),
              new Function<Identity, String>() {
                @Override
                public String apply(Identity identity) {
                  return IDENTITY_STR_VALUE_FUNCTION.apply(identity);
                }
              }));
      bindingPbList.add(bindingPb);
    }
    policyPb.setBindings(bindingPbList);
    policyPb.setEtag(policy.getEtag());
    policyPb.setVersion(policy.getVersion());
    return policyPb;
  }
}