package org.jivesoftware.openfire.plugin.util.cache;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.config.MemcacheProtocolConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.RestApiConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cluster.Member; // Updated import
import com.hazelcast.cluster.Cluster; // Updated import
import org.jivesoftware.openfire.JMXManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.cluster.ClusterEventListener;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.cluster.ClusterNodeInfo;
import org.jivesoftware.openfire.cluster.NodeID;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginClassLoader;
import org.jivesoftware.openfire.plugin.HazelcastPlugin;
import org.jivesoftware.openfire.plugin.util.cluster.HazelcastClusterNodeInfo;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.SystemProperty;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.util.cache.CacheFactoryStrategy;
import org.jivesoftware.util.cache.CacheWrapper;
import org.jivesoftware.util.cache.ClusterTask;
import org.jivesoftware.util.cache.ExternalizableUtil;
import org.jivesoftware.util.cache.ExternalizableUtilStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ClusteredCacheFactory implements CacheFactoryStrategy {

    // ... (rest of the code remains unchanged)

    @Override
    public boolean startCluster() {
        logger.info("Starting hazelcast clustering");
        state = State.starting;

        // Set the serialization strategy to use for transmitting objects between node clusters
        serializationStrategy = ExternalizableUtil.getInstance().getStrategy();
        ExternalizableUtil.getInstance().setStrategy(new ClusterExternalizableUtil());

        // Store previous class loader (in case we change it)
        final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader loader = new ClusterClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        int retry = 0;
        do {
            try {
                final Config config = new ClasspathXmlConfig(HAZELCAST_CONFIG_FILE.getValue());
                final NetworkConfig networkConfig = config.getNetworkConfig();
                if (!HAZELCAST_MEMCACHE_ENABLED.getValue()) {
                    networkConfig.setMemcacheProtocolConfig(new MemcacheProtocolConfig().setEnabled(false));
                }
                if (!HAZELCAST_REST_ENABLED.getValue()) {
                    networkConfig.setRestApiConfig(new RestApiConfig().setEnabled(false));
                }
                final MemberAttributeConfig memberAttributeConfig = config.getMemberAttributeConfig();
                memberAttributeConfig.setStringAttribute(HazelcastClusterNodeInfo.HOST_NAME_ATTRIBUTE, XMPPServer.getInstance().getServerInfo().getHostname());
                memberAttributeConfig.setStringAttribute(HazelcastClusterNodeInfo.NODE_ID_ATTRIBUTE, XMPPServer.getInstance().getNodeID().toString());
                config.setInstanceName("openfire");
                config.setClassLoader(loader);
                if (JMXManager.isEnabled() && HAZELCAST_JMX_ENABLED.getValue()) {
                    config.setProperty("hazelcast.jmx", "true");
                    config.setProperty("hazelcast.jmx.detailed", "true");
                }
                hazelcast = Hazelcast.newHazelcastInstance(config);
                cluster = hazelcast.getCluster();
                state = State.started;
                // CacheFactory is now using clustered caches. We can add our listeners.
                clusterListener = new ClusterListener(cluster);
                clusterListener.joinCluster();
                lifecycleListener = hazelcast.getLifecycleService().addLifecycleListener(clusterListener);
                membershipListener = cluster.addMembershipListener(clusterListener);
                logger.info("Hazelcast clustering started");
                break;
            } catch (final Exception e) {
                cluster = null;
                if (retry < CLUSTER_STARTUP_RETRY_COUNT.getValue()) {
                    logger.warn("Failed to start clustering (" + e.getMessage() + "); " +
                        "will retry in " + StringUtils.getFullElapsedTime(CLUSTER_STARTUP_RETRY_TIME.getValue()));
                    try {
                        Thread.sleep(CLUSTER_STARTUP_RETRY_TIME.getValue().toMillis());
                    } catch (final InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    logger.error("Unable to start clustering - continuing in local mode", e);
                    state = State.stopped;
                }
            }
        } while (retry++ < CLUSTER_STARTUP_RETRY_COUNT.getValue() && !Thread.currentThread().isInterrupted());

        if (oldLoader != null) {
            // Restore previous class loader
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
        return cluster != null;
    }

    // ... (rest of the code remains unchanged)

    @Override
    public List<ClusterNodeInfo> getClusterNodesInfo() {
        return clusterListener == null ? Collections.emptyList() : clusterListener.getClusterNodesInfo();
    }

    @Override
    public int getMaxClusterNodes() {
        // No longer depends on license code so just return a big number
        return 10000;
    }

    @Override
    public byte[] getSeniorClusterMemberID() {
        if (cluster != null && !cluster.getMembers().isEmpty()) {
            final Member oldest = cluster.getMembers().iterator().next();
            return getNodeID(oldest).toByteArray();
        } else {
            return null;
        }
    }

    @Override
    public byte[] getClusterMemberID() {
        if (cluster != null) {
            return getNodeID(cluster.getLocalMember()).toByteArray();
        } else {
            return null;
        }
    }

    // ... (rest of the code remains unchanged)

    private Member getMember(final byte[] nodeID) {
        final NodeID memberToFind = NodeID.getInstance(nodeID);
        for (final Member member : cluster.getMembers()) {
            if (memberToFind.equals(getNodeID(member))) {
                return member;
            }
        }
        return null;
    }

    // ... (rest of the code remains unchanged)

}