package org.jivesoftware.openfire.plugin.util.cache;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.spi.properties.GroupProperty;
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
 */
public class ClusteredCacheFactory implements CacheFactoryStrategy {

    // ... (rest of the class remains unchanged)

    @Override
    public boolean startCluster() {
        logger.info("Starting hazelcast clustering");
        state = State.starting;

        // ... (rest of the method remains unchanged)

        // Update the running state of the cluster
        state = State.started;
        // CacheFactory is now using clustered caches. We can add our listeners.
        clusterListener = new ClusterListener(cluster);
        clusterListener.joinCluster();
        lifecycleListener = hazelcast.getLifecycleService().addLifecycleListener(clusterListener);
        membershipListener = cluster.addMembershipListener(clusterListener);
        logger.info("Hazelcast clustering started");
        break;
    }

    // ... (rest of the class remains unchanged)

    private static class ClusterLock implements Lock {

        private final Serializable key;
        private final ClusteredCache clusteredCache;

        ClusterLock(final Serializable key, final ClusteredCache clusteredCache) {
            this.key = key;
            this.clusteredCache = clusteredCache;
        }

        @Override
        public void lock() {
            clusteredCache.lock(key, -1);
        }

        @Override
        public void lockInterruptibly() {
            clusteredCache.lock(key, -1);
        }

        @Override
        public boolean tryLock() {
            return clusteredCache.lock(key, 0);
        }

        @Override
        public boolean tryLock(final long time, final TimeUnit unit) {
            return clusteredCache.lock(key, unit.toMillis(time));
        }

        @Override
        public void unlock() {
            clusteredCache.unlock(key);
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
                logger.debug("CallableTask[{}] result: {}", task.getClass().getName, task.getResult());
                return task.getResult();
            } catch (final Exception e) {
                logger.error("Unexpected exception running CallableTask[{}]", task.getClass().getName, e);
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
        final LifecycleListener lifecycleListener = new LifecycleListener() {
            @Override
            public void stateChanged(final LifecycleEvent event) {
                if (event.getState() == LifecycleEvent.State.SHUTDOWN) {
                    leftClusterSemaphore.release();
                }
            }
        };
        try {
            // Add a listener at the ultimate end of the list of all listeners, to detect that left-cluster event handling
            // has been invoked for all before proceeding.
            ClusterManager.addListener(lifecycleListener, Integer.MAX_VALUE);
            logger.debug("Firing leftCluster() event");
            ClusterManager.fireLeftCluster();
            logger.debug("Waiting for leftCluster() event to be called [timeout={}]", StringUtils.getFullElapsedTime(timeout));
            if (!leftClusterSemaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                logger.warn("Timeout waiting for leftCluster() event to be called [timeout={}]", StringUtils.getFullElapsedTime(timeout);
            }
        } catch (final Exception e) {
            logger.error("Unexpected exception waiting for clustering to shut down", e);
        } finally {
            ClusterManager.removeListener(lifecycleListener);
        }
    }
}