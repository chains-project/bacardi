/*
 * Copyright (C) 2007-2009 Jive Software. All rights reserved.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.plugin.util.cache;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MemberAttributeConfig;
import com.hazelcast.cluster.MemberSelector;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.cluster.impl.ClusterServiceImpl;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.RestApiConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
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
import org.slf4j.LoggerFactoryFactory;

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

/**
 * CacheFactory implementation to use when using Hazelcast in cluster mode.
 *
 * @author Tom Evans
 * @author Gaston Dombiak
 */
public class ClusteredCacheFactory implements CacheFactoryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ClusteredCacheFactory.class);
    public static final String PLUGIN_NAME = "hazelcast";

    /**
     * Keep serialization strategy the server was using before clustering was loaded.
     */
    private ExternalizableUtilStrategy serializationStrategy;

    /**
     * Storage for cache statistics
     */
    private static Map<String, Map<String, long[]>> cacheStats;

    private static HazelcastInstance hazelcast = null;
    private static ClusterServiceImpl cluster = null;
    private ClusterListener clusterListener;
    private String lifecycleListener;
    private String membershipListener;

    public ClusteredCacheFactory() {
        pluginClassLoaderWarnings = CacheFactory.createLocalCache("PluginClassLoader Warnings for Clustered Tasks");
        pluginClassLoaderWarnings.setMaxLifetime(Duration.ofHours(1).toMillis()); // Minimum duration between logged warnings.
    }

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
                    networkConfig.setRestApiConfig(new RestApiConfig().setEnabled(false);
                }
                final MemberAttributeConfig memberAttributeConfig = config.getMemberAttributeConfig();
                memberAttributeConfig.setAttribute(HazelcastClusterNodeInfo.HOST_NAME_ATTRIBUTE, XMPPServer.getInstance().getServerInfo().getHostname());
                memberAttributeConfig.setAttribute(HazelcastClusterNodeInfo.NODE_ID_ATTRIBUTE, XMPPServer.getInstance().getNodeID().toString());
                config.setInstanceName("openfire");
                config.setClassLoader(loader);
                if (JMXManager.isEnabled() && HAZELCAST_JMX_ENABLED.getValue()) {
                    config.setProperty("hazelcast.jmx", "true");
                    config.setProperty("hazelcast.jmx.detailed", "true");
                }
                hazelcast = Hazelcast.newHazelcastInstance(config);
                cluster = (ClusterServiceImpl) hazelcast.getCluster();
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

    @Override
    public void stopCluster() {
        // Stop the cache services.
        cacheStats = null;
        // Update the running state of the cluster
        state = State.stopped;

        // Fire the leftClusterEvent before we leave the cluster
        fireLeftClusterAndWaitToComplete(Duration.ofSeconds(30));
        // Stop the cluster
        hazelcast.getLifecycleService().removeLifecycleListener(lifecycleListener);
        cluster.removeMembershipListener(membershipListener);
        Hazelcast.shutdownAll();
        cluster = null;
        lifecycleListener = null;
        membershipListener = null;
        clusterListener = null;

        // Reset packet router to use to deliver packets to remote cluster nodes
        XMPPServer.getInstance().getRoutingTable().setRemotePacketRouter(null);
        // Reset the session locator to use
        XMPPServer.getInstance().setRemoteSessionLocator(null);
        // Set the old serialization strategy was using before clustering was loaded
        ExternalizableUtil.getInstance().setStrategy(serializationStrategy);
    }

    @Override
    public Cache createCache(final String name) {
        // Check if cluster is being started up
        while (state == State.starting) {
            // Wait until cluster is fully started (or failed)
            try {
                Thread.sleep(250);
            } catch (final InterruptedException e) {
                // Ignore
            }
        }
        if (state == State.stopped) {
            throw new IllegalStateException("Cannot create clustered cache when not in a cluster");
        }
        // Determine the time to live. Note that in Hazelcast 0 means "forever", not -1
        final long openfireLifetimeInMilliseconds = CacheFactory.getMaxCacheLifetime(name);
        final int hazelcastLifetimeInSeconds = openfireLifetimeInMilliseconds < 0 ? 0 : Math.max((int) (openfireLifetimeInMilliseconds / 1000), 1);
        // Determine the max cache size. Note that in Hazelcast the max cache size must be positive and is in megabytes
        final long openfireMaxCacheSizeInBytes = CacheFactory.getMaxCacheSize(name);
        final int hazelcastMaxCacheSizeInMegaBytes = openfireMaxCacheSizeInBytes < 0 ? Integer.MAX_VALUE : Math.max((int) openfireMaxCacheSizeInBytes / 1024 / 1024, 1);
        // It's only possible to create a dynamic config if a static one doesn't already exist
        final MapConfig staticConfig = hazelcast.getConfig().getMapConfigOrNull(name);
        if (staticConfig == null) {
            final MapConfig dynamicConfig = new MapConfig(name);
            dynamicConfig.setTimeToLiveSeconds(hazelcastLifetimeInSeconds);
            dynamicConfig.setMaxSizeConfig(new MaxSizeConfig(hazelcastMaxCacheSizeInMegaBytes, MaxSizeConfig.MaxSizePolicy.USED_HEAP_SIZE));
            logger.debug("Creating dynamic map config for cache={}, dynamicConfig={}", name, dynamicConfig);
            hazelcast.getConfig().addMapConfig(dynamicConfig);
        } else {
            logger.debug("Static configuration already exists for cache={}, staticConfig={}", name, staticConfig);
        }
        // TODO: Better genericize this method in CacheFactoryStrategy so the signature is getLock(final Serializable key, Cache<Serializable, Serializable> cache)
        @SuppressWarnings("unchecked") final ClusterLock clusterLock = new ClusterLock((Serializable) key, (ClusteredCache<Serializable, ?>) cache);
        return clusterLock;
    }

    @Override
    public void updateCacheStats(final Map<String, Cache> caches) {
        if (!caches.isEmpty() && cluster != null) {
            // Create the cacheStats map if necessary.
            if (cacheStats == null) {
                cacheStats = hazelcast.getMap("opt-$cacheStats");
            }
            final String uid = getNodeID(cluster.getLocalMember()).toString();
            final Map<String, long[]> stats = new HashMap<>();
            for (final String cacheName : caches.keySet()) {
                final Cache cache = caches.get(cacheName);
                // The following information is published:
                // current size, max size, num elements, cache
                // hits, cache misses.
                final long[] info = new long[5];
                info[0] = cache.getLongCacheSize();
                info[1] = cache.getMaxCacheSize();
                info[2] = cache.size();
                info[3] = cache.getCacheHits();
                info[4] = cache.getCacheMisses();
                stats.put(cacheName, info);
            }
            // Publish message
            cacheStats.put(uid, stats);
        }
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public Lock getLock(final Object key, Cache cache) {
        if (cache instanceof CacheWrapper) {
            cache = ((CacheWrapper) cache).getWrappedCache();
        }
        // TODO: Update CacheFactoryStrategy so the signature is getLock(final Serializable key, Cache<Serializable, Serializable> cache)
        @SuppressWarnings("unchecked") final ClusterLock clusterLock = new ClusterLock((Serializable) key, (ClusteredCache<Serializable, ?>) cache);
        return clusterLock;
    }

    /**
     * ClusterTasks that are executed should not be provided by a plugin. These will cause issues related to class
     * loading when the providing plugin is reloaded. This method verifies if an instance of a task is
     * loaded by a plugin class loader, and logs a warning to the log files when it is. The amount of warnings logged is
     * limited by a time interval.
     *
     * @param o the instance for which to verify the class loader
     * @see <a href="https://github.com/igniterealtime/openfire-hazelcast-plugin/issues/74">Issue #74: Warn against usage of plugin-provided classes in Hazelcast</a>
     */
    protected <T extends ClusterTask<?>> void checkForPluginClassLoader(final T o) {
        if (o != null && o.getClass().getClassLoader() instanceof PluginClassLoader
            && !pluginClassLoaderWarnings.containsKey(o.getClass().getName()) )
        {
            // Try to determine what plugin loaded the offending class.
            String pluginName = null;
            try {
                final Collection<Plugin> plugins = XMPPServer.getInstance().getPluginManager().getPlugins();
                for (final Plugin plugin : plugins) {
                    final PluginClassLoader pluginClassloader = XMPPServer.getInstance().getPluginManager().getPluginClassloader(plugin);
                    if (o.getClass().getClassLoader().equals(pluginClassloader)) {
                        pluginName = XMPPServer.getInstance().getPluginManager().getCanonicalName(plugin);
                        break;
                    }
                }
            } catch (Exception e) {
                logger.debug("An exception occurred while trying to determine the plugin class loader that loaded an instance of {}", o.getClass(), e);
            }
            logger.warn("An instance of {} that is executed as a cluster task. This will cause issues when reloading " +
                    "the plugin that provides this class. The plugin implementation should be modified.",
                pluginName != null ? o.getClass() + " (provided by plugin " + pluginName + ")" : o.getClass());
            pluginClassLoaderWarnings.put(o.getClass().getName(), Instant.now()); // Note that this Instant is unused.
        }
    }

    private static class ClusterLock implements Lock {

        private final Serializable key;
        private final ClusteredCache<Serializable, ?> cache;

        ClusterLock(final Serializable key, final ClusteredCache<Serializable, ?> cache) {
            this.key = key;
            this.cache = cache;
        }

        @Override
        public void lock() {
            cache.lock(key, -1);
        }

        @Override
        public void lockInterruptibly() {
            cache.lock(key, -1);
        }

        @Override
        public boolean tryLock() {
            return cache.lock(key, 0);
        }

        @Override
        public boolean tryLock(final long time, final TimeUnit unit) {
            return cache.lock(key, unit.toMillis(time));
        }

        @Override
        public void unlock() {
            cache.unlock(key);
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    private static class CallableTask<V> implements Callable<V>, Serializable {
        private static final long serialVersionUID = -8761271979427214681L;
        private final ClusterTask<V> task;

        CallableTask(final ClusterTask<V> task) {
            this.task = task;
        }

        @Override
        public V call() {
            try {
                task.run();
                logger.trace("CallableTask[{}] result: {}", task.getClass().getName(), task.getResult());
                return task.getResult();
            } catch (final Exception e) {
                logger.error("Unexpected exception running CallableTask[{}]", task.getClass().getName(), e);
                throw e;
            }
        }
    }

    private enum State {
        stopped,
        starting,
        started
    }

    public static NodeID getNodeID(final Member member) {
        return NodeID.getInstance(member.getAttribute(HazelcastClusterNodeInfo.HOST_NAME_ATTRIBUTE).getBytes(StandardCharsets.UTF_8));
    }

    static void fireLeftClusterAndWaitToComplete(final Duration timeout) {
        final Semaphore leftClusterSemaphore = new Semaphore(0);
        final MembershipListener clusterEventListener = new MembershipListener() {
            @Override
            public void memberAdded(MembershipEvent event) {
            }

            @Override
            public void memberRemoved(MembershipEvent event) {
            }

            @Override
            public void memberAttributeChanged(MembershipEvent event) {
            }

            @Override
            public void memberListChanged(MembershipEvent event) {
                leftClusterSemaphore.release();
            }

            @Override
            public void memberVersionMismatch(MembershipEvent event) {
            }

            @Override
            public void memberRemoved(MembershipEvent event) {
            }

            @Override
            public void memberAdded(MembershipEvent event) {
            }
        };
        try {
            // Add a listener at the ultimate end of the list of all listeners, to detect that left-cluster event handling
            // has been invoked for all before proceeding.
            ClusterManager.addListener(clusterEventListener, Integer.MAX_VALUE);
            logger.debug("Firing leftCluster() event");
            ClusterManager.fireLeftCluster();
            logger.debug("Waiting for leftCluster() event to be called [timeout={}]", StringUtils.getFullElapsedTime(timeout));
            if (!leftClusterSemaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                logger.warn("Timeout waiting for leftCluster() event to be called [timeout={}]", StringUtils.getFullElapsedTime(timeout);
            }
        } catch (final Exception e) {
            logger.error("Unexpected exception waiting for clustering to shut down", e);
        } finally {
            ClusterManager.removeListener(clusterEventListener);
        }
    }

    private Member getMember(final byte[] nodeID) {
        final NodeID memberToFind = NodeID.getInstance(nodeID);
        for (final Member member : cluster.getMembers()) {
            if (memberToFind.equals(getNodeID(member)) {
                return member;
            }
        }
        return null;
    }

    private static class ClusterListener implements MembershipListener, LifecycleListener {

        private final ClusterServiceImpl cluster;

        ClusterListener(final ClusterServiceImpl cluster) {
            this.cluster = cluster;
        }

        public void joinCluster() {
            // Implementation for joining the cluster
        }

        @Override
        public void memberAdded(MembershipEvent event) {
            // Implementation for memberAdded
        }

        @Override
        public void memberRemoved(MembershipEvent event) {
            // Implementation for memberRemoved
        }

        @Override
        public void memberAttributeChanged(MembershipEvent event) {
            // Implementation for memberAttributeChanged
        }

        @Override
        public void memberListChanged(MembershipEvent event) {
            // Implementation for memberListChanged
        }

        @Override
        public void memberVersionMismatch(MembershipEvent event) {
            // Implementation for memberVersionMismatch
        }

        @Override
        public void memberRemoved(MembershipEvent event) {
            // Implementation for memberRemoved
        }

        @Override
        public void memberAdded(MembershipEvent event) {
            // Implementation for memberAdded
        }

        @Override
        public void stateChanged(LifecycleEvent event) {
            // Implementation for stateChanged
        }
    }
}
