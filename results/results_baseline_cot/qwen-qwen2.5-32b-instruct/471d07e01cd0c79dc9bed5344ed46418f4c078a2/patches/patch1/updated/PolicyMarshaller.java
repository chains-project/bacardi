package com.google.cloud.resourcemanager;

import com.google.cloud.resourcemanager.v3.Binding; // Assuming the new package structure
import com.google.cloud.resourcemanager.v3.Policy; // Assuming the new package structure
import com.google.cloud.Identity;
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
    extends Marshaller<com.google.cloud.resourcemanager.v3.Policy> { // Adjusted package

  static final PolicyMarshaller INSTANCE = new PolicyMarshaller();

  private PolicyMarshaller() {}

  private static class Builder extends Policy.Builder {

    private Builder(Map<Role, Set<Identity>> bindings, String etag, Integer version) {
      setBindings(bindings);
      setEtag(etag);
      if (version != null) {
        setVersion(version);
      }
    }
  }

  @Override
  protected Policy fromPb(com.google.cloud.resourcemanager.v3.Policy policyPb) { // Adjusted package
    Map<Role, Set<Identity>> bindings = new HashMap<>();
    if (policyPb.getBindingsList() != null) { // Adjusted method name
      for (Binding bindingPb : policyPb.getBindingsList()) { // Adjusted method name
        bindings.put(
            Role.of(bindingPb.getRole()),
            ImmutableSet.copyOf(
                Lists.transform(
                    bindingPb.getMembersList(), // Adjusted method name
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
  protected com.google.cloud.resourcemanager.v3.Policy toPb(Policy policy) { // Adjusted package
    com.google.cloud.resourcemanager.v3.Policy policyPb = // Adjusted package
        com.google.cloud.resourcemanager.v3.Policy.newBuilder().build(); // Adjusted package
    List<Binding> bindingPbList = new LinkedList<>();
    for (Map.Entry<Role, Set<Identity>> binding : policy.getBindings().entrySet()) {
      Binding bindingPb = Binding.newBuilder() // Adjusted method name
          .setRole(binding.getKey().getValue())
          .addAllMembers(
              Lists.transform(
                  new ArrayList<>(binding.getValue()),
                  new Function<Identity, String>() {
                    @Override
                    public String apply(Identity identity) {
                      return IDENTITY_STR_VALUE_FUNCTION.apply(identity);
                    }
                  }))
          .build();
      bindingPbList.add(bindingPb);
    }
    policyPb = policyPb.toBuilder().addAllBindings(bindingPbList).build(); // Adjusted method name
    policyPb = policyPb.toBuilder().setEtag(policy.getEtag()).build(); // Adjusted method name
    policyPb = policyPb.toBuilder().setVersion(policy.getVersion()).build(); // Adjusted method name
    return policyPb;
  }
}