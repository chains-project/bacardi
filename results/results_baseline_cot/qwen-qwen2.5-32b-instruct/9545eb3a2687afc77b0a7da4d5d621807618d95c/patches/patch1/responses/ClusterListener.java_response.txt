package org.jivesoftware.openfire.plugin.util.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.IMap;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.cluster.ClusterNodeInfo;
import org.jivesoftware.openfire.cluster.NodeID;
import org.jivesoftware.openfire.muc.cluster.NewClusterMemberJoinedTask;
import org.jivesoftware.openfire.plugin.util.cluster.HazelcastClusterNodeInfo;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.util.cache.CacheWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClusterListener reacts to membership changes in the cluster. It takes care of cleaning up the state
 * of the routing table and the sessions within it when a node which manages those sessions goes down.
 */
public class ClusterListener implements MembershipListener, LifecycleListener, HazelcastInstanceAware {

    private static final Logger logger = LoggerFactory.getLogger(ClusterListener.class);

    private boolean seniorClusterMember = false;

    private final Map<Cache<?,?>, EntryListener> entryListeners = new HashMap<>();
    
    private final HazelcastInstance cluster;
    private final Map<NodeID, ClusterNodeInfo> clusterNodesInfo = new ConcurrentHashMap<>();

    public ClusterListener(final HazelcastInstance cluster) {

    {
        this.cluster = cluster;
        for (final Member member : cluster.getMembers()) {
            clusterNodesInfo.put(ClusteredCacheFactory.getNodeID(member),
                    new HazelcastClusterNodeInfo(member, cluster.getClusterTime()));
        }
    }

    private void addEntryListener(final Cache<?, ?> cache, final EntryListener listener) {
        if (cache instanceof CacheWrapper) {
            final Cache wrapped = ((CacheWrapper)cache).getWrappedCache();
            if (wrapped instanceof ClusteredCache) {
                ((ClusteredCache)wrapped).addEntryListener(listener);
                // Keep track of the listener that we added to the cache
                entryListeners.put(cache, listener);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isDone() {
        return done;
    }

    synchronized void joinCluster() {
        if (isDone()) { // not a cluster member
            return;
        }
        clusterMember = true;
        seniorClusterMember = isSeniorClusterMember();

        // Trigger event. Wait until the listeners have processed the event. Caches will be populated
        // again with local content.
        ClusterManager.fireJoinedCluster(false);

        if (!XMPPServer.getInstance().isShuttingDown()) {
            // Remove traces of directed presences sent from local entities to handlers that no longer exist.
            // At this point c2s sessions are gone from the routing table so we can identify expired sessions
            XMPPServer.getInstance().getPresenceUpdateHandler().removedExpiredPresences();
        }
        logger.info("Joined cluster. XMPPServer node={}, Hazelcast UUID={}, seniorClusterMember={}",
            new Object[]{ClusteredCacheFactory.getNodeID(cluster.getLocalMember()), cluster.getLocalMember().getUuid(), seniorClusterMember});
        done = false;
    }

    boolean isSeniorClusterMember() {
        // first cluster member is the oldest
        final Iterator<Member> members = cluster.getMembers().iterator();
        return members.next().getUuid().equals(cluster.getLocalMember().getUuid());
    }

    private synchronized void leaveCluster() {
        if (isDone()) { // not a cluster member
            return;
        }
        clusterMember = false;
        final boolean wasSeniorClusterMember = seniorClusterMember;
        seniorClusterMember = false;

        // Trigger event. Wait until the listeners have processed the event. Caches will be populated
        // again with local content.
        ClusterManager.fireLeftCluster();

        if (!XMPPServer.getInstance().isShuttingDown()) {
            // Remove traces of directed presences sent from local entities to handlers that no longer exist.
            // At this point c2s sessions are gone from the routing table so we can identify expired sessions
            XMPPServer.getInstance().getPresenceUpdateHandler().removedExpiredPresences();
        }
        logger.info("Left cluster. XMPPServer node={}, Hazelcast UUID={}, wasSeniorClusterMember={}",
            new Object[]{ClusteredCacheFactory.getNodeID(cluster.getLocalMember()), cluster.getLocalMember().getUuid(), wasSeniorClusterMember});
        done = true;
    }

    @Override
    public void memberAdded(final MembershipEvent event) {
        logger.info("Received a Hazelcast memberAdded event {}", event);

        isSenior = isSeniorClusterMember();
        final NodeID nodeID = ClusteredCacheFactory.getNodeID(event.getMember());

        if (event.getMember().localMember()) {
            logger.info("Leaving cluster: " + nodeID);
            // This node may have realized that it got kicked out of the cluster
            leaveCluster();
        } else {
            // Trigger event that a node left the cluster
            ClusterManager.fireLeftCluster(nodeID.toByteArray());

            if (!seniorClusterMember && isSeniorClusterMember()) {
                seniorClusterMember = true;
                ClusterManager.fireMarkedAsSeniorClusterMember();
            }

            // Remove traces of directed presences sent from local entities to handlers that no longer exist.
            // At this point c2s sessions are gone from the routing table so we can identify expired sessions
            XMPPServer.getInstance().getPresenceUpdateHandler().removedExpiredPresences();
        }
        // Delete nodeID instance (release from memory)
        NodeID.deleteInstance(nodeID.toByteArray());
        clusterNodesInfo.remove(nodeID);
    }

    /**
     * Blocks the current thread until the cluster cache is guaranteed to support clustering. This is especially useful
     * for executing cluster tasks immediately after joining.
     */
    private boolean waitForClusterCacheToBeInstalled() {
        boolean failed = false;
        if (!ClusteredCacheFactory.PLUGIN_NAME.equals(CacheFactory.getPluginName())) {
            logger.debug("This node now joined a cluster, but the cache factory has not been swapped to '{}' yet. Waiting for that to happen.", ClusteredCacheFactory.PLUGIN_NAME);
            LocalTime deadLine = LocalTime.now().plusMinutes(10L);
            while (!ClusteredCacheFactory.PLUGIN_NAME.equals(CacheFactory.getPluginName()) && deadLine.isAfter(LocalTime.now())) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    logger.trace("Thread was interrupted while waiting for cache strategy to change.");
                    failed = true;
                    break;
                }
            }
            if (!deadLine.isAfter(LocalTime.now())) {
                failed = true;
                logger.warn("Cache factory was not swapped to '{}', but still remains '{}' after a 10 minute wait. Cluster join is not guaranteed to have completed.", ClusteredCacheFactory.PLUGIN_NAME, CacheFactory.getPluginName());
            }
            logger.debug("Cache factory has been swapped to '{}'. Cluster join is considered complete.", ClusteredCacheFactory.PLUGIN_NAME);
        }

        return !failed;
    }

    @Override
    public void memberRemoved(final MembershipEvent event) {
        logger.info("Received a Hazelcast memberRemoved event {}", event);

        isSenior = isSeniorClusterMember();
        final NodeID nodeID = ClusteredCacheFactory.getNodeID(event.getMember());

        if (event.getMember().localMember()) {
            logger.info("Leaving cluster: " + nodeID);
            // This node may have realized that it got kicked out of the cluster
            leaveCluster();
        } else {
            // Trigger event that a node left the cluster
            ClusterManager.fireLeftCluster(nodeID.toByteArray());

            if (!seniorClusterMember && isSeniorClusterMember()) {
                seniorClusterMember = true;
                ClusterManager.fireMarkedAsSeniorClusterMember();
            }

            // Remove traces of directed presences sent from local entities to handlers that no longer exist.
            // At this point c2s sessions are gone from the routing table so we can identify expired sessions
            XMPPServer.getInstance().getPresenceUpdateHandler().removedExpiredPresences();
        }
        // Delete nodeID instance (release from memory)
        NodeID.deleteInstance(nodeID.toByteArray());
        clusterNodesInfo.remove(nodeID);
    }

    @Override
    public void memberAttributeChanged(final MemberAttributeEvent event) {
        logger.info("Received a Hazelcast memberAttributeChanged event {}", event);
        isSenior = isSeniorClusterMember();
        final ClusterNodeInfo priorNodeInfo = clusterNodesInfo.get(ClusteredCacheFactory.getNodeID(event.getMember()));
        clusterNodesInfo.put(ClusteredCacheFactory.getNodeID(event.getMember()),
                new HazelcastClusterNodeInfo(event.getMember(), priorNodeInfo.getJoinedTime()));
    }

    boolean isClusterMember() {
        return clusterMember;
    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.cluster = hazelcastInstance;
    }
}