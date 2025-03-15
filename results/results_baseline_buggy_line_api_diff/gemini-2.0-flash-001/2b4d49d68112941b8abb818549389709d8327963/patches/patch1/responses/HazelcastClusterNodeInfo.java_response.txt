/*
 * Copyright (C) 1999-2009 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License governing permissions and limitations under the License.
 */

package org.jivesoftware.openfire.plugin.util.cluster;

import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.cluster.ClusterNodeInfo;
import org.jivesoftware.openfire.cluster.NodeID;
import org.jivesoftware.openfire.plugin.util.cache.ClusteredCacheFactory;

import com.hazelcast.cluster.Member;

import java.net.InetSocketAddress;

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
        String hostnameAttribute = null;
        try {
            hostnameAttribute = member.getStringAttribute(HOST_NAME_ATTRIBUTE);
        } catch (NoSuchMethodError e) {
            // Handle the case where getStringAttribute is not available
            hostnameAttribute = null; // Or provide a default value
        }

        InetSocketAddress socketAddress = null;
        try {
            socketAddress = member.getSocketAddress();
        } catch (NoSuchMethodError e) {
            // Handle the case where getSocketAddress() with no arguments is not available.
            // Attempt to use getSocketAddress(EndpointQualifier) if available.
            try {
                java.lang.reflect.Method getSocketAddressMethod = member.getClass().getMethod("getSocketAddress", com.hazelcast.instance.EndpointQualifier.class);
                socketAddress = (InetSocketAddress) getSocketAddressMethod.invoke(member, (Object) null); // Or pass an appropriate EndpointQualifier if needed
            } catch (Exception ex) {
                // If even the alternative method fails, handle the error appropriately.
                socketAddress = null; // Or provide a default value
            }
        }

        String hostString = (socketAddress != null) ? socketAddress.getHostString() : "unknown";

        this.hostname = (hostnameAttribute != null ? hostnameAttribute : "unknown") + " (" + hostString + ")";
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