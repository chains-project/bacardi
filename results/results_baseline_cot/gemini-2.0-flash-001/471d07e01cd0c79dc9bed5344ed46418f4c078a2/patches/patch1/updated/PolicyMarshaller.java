package com.google.cloud.resourcemanager;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.iam.v1.Binding;
import com.google.iam.v1.PolicyProto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @deprecated v3 GAPIC client of ResourceManager is now available */
@Deprecated
final class PolicyMarshaller
    extends Marshaller<com.google.iam.v1.Policy> {

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
  protected Policy fromPb(com.google.iam.v1.Policy policyPb) {
    Map<Role, Set<Identity>> bindings = new HashMap<>();
    if (policyPb.getBindingsList() != null) {
      for (Binding bindingPb : policyPb.getBindingsList()) {
        bindings.put(
            Role.of(bindingPb.getRole()),
            ImmutableSet.copyOf(
                Lists.transform(
                    bindingPb.getMembersList(),
                    new Function<String, Identity>() {
                      @Override
                      public Identity apply(String s) {
                        return IDENTITY_VALUE_OF_FUNCTION.apply(s);
                      }
                    })));
      }
    }
    String etag = policyPb.getEtag().isEmpty() ? null : policyPb.getEtag().toStringUtf8();
    return new Builder(bindings, etag, policyPb.getVersion()).build();
  }

  @Override
  protected com.google.iam.v1.Policy toPb(Policy policy) {
    com.google.iam.v1.Policy.Builder policyPb = com.google.iam.v1.Policy.newBuilder();
    List<Binding> bindingPbList = new LinkedList<>();
    for (Map.Entry<Role, Set<Identity>> binding : policy.getBindings().entrySet()) {
      Binding bindingPb = Binding.newBuilder().setRole(binding.getKey().getValue()).addAllMembers(
          Lists.transform(
              new ArrayList<>(binding.getValue()),
              new Function<Identity, String>() {
                @Override
                public String apply(Identity identity) {
                  return IDENTITY_STR_VALUE_FUNCTION.apply(identity);
                }
              })).build();
      bindingPbList.add(bindingPb);
    }
    policyPb.addAllBindings(bindingPbList);
    if (policy.getEtag() != null) {
      policyPb.setEtag(com.google.protobuf.ByteString.copyFromUtf8(policy.getEtag()));
    }
    if (policy.getVersion() != null) {
      policyPb.setVersion(policy.getVersion());
    }
    return policyPb.build();
  }
}