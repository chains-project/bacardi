package org.jivesoftware.openfire.plugin.util.cluster;

import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.cluster.ClusterNodeInfo;
import org.jivesoftware.openfire.cluster.NodeID;
import org.jivesoftware.openfire.plugin.util.cache.ClusteredCacheFactory;

import com.hazelcast.core.MemberAdapter; // Updated import
import com.hazelcast.core.Member; // Updated import

/**
 * Cluster Node information as provided by Hazelcast.
 *
 * @author Tom Evans
 * @author Gaston Dombiak
 */
public class HazelcastClusterNodeInfo implements ClusterNodeInfo {

    public static final String HOST_NAME_ATTRIBUTE = "hostname";
    public static final String NODE_ID_ATTRIBUTE = "node-id";
    private final String hostname;
    private final NodeID nodeID;
    private final long joinedTime;
    private final boolean seniorMember;

    public HazelcastClusterNodeInfo(final Member member, final long joinedTime) {
        this.hostname = member.getStringAttribute(HOST_NAME_ATTRIBUTE) + " (" + member.getSocketAddress().getHostString() + ")";
        this.nodeID = ClusteredCacheFactory.getNodeID(member);
        this.joinedTime = joinedTime;
        this.seniorMember = ClusterManager.getSeniorClusterMember().equals(nodeID);
    }

    public String getHostName() {
        return hostname;
    }

    public NodeID getNodeID() {
        return nodeID;
    }

    public long getJoinedTime() {
        return joinedTime;
    }

    public boolean isSeniorMember() {
        return seniorMember;
    }
}