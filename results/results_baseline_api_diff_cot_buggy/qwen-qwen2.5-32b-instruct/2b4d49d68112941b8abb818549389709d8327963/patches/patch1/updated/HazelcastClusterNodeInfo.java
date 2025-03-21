<repair_strategy>
1. Replace `com.hazelcast.core.Member` with `com.hazelcast.cluster.Member`.
2. Replace `member.getStringAttribute(HOST_NAME_ATTRIBUTE)` with `member.getAttribute(HOST_NAME_ATTRIBUTE)`.
3. Replace `member.getSocketAddress()` with `member.getEndpoint().getSocketAddress()`.
</repair_strategy>

