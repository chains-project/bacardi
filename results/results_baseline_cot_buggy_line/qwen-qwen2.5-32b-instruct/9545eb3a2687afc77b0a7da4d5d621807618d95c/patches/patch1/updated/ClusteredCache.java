package org.jivesoftware.openfire.plugin.util.cache;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.impl.MapEventImpl;
import com.hazelcast.monitor.LocalMapStats;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.cluster.ClusteredCacheEntryListener;
import org.jivesoftware.openfire.cluster.NodeID;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginClassLoader;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ClusteredCache<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final Logger logger;

    private final Set<String> listeners = ConcurrentHashMap.newKeySet();

    final IMap<K, V> map;
    private String name;
    private long numberOfGets = 0;

    protected ClusteredCache(final String name, final IMap<K, V> cache) {
        this.map = cache;
        this.name = name;
        logger = LoggerFactory.getLogger(ClusteredCache.class.getName() + "[cache: "+name+"]");
    }

    void addEntryListener(final EntryListener<K, V> listener) {
        listeners.add(map.addEntryListener(listener, false));
    }

    public String addClusteredCacheEntryListener(@Nonnull final ClusteredCacheEntryListener<K, V> clusteredCacheEntryListener, final boolean includeValues, final boolean includeEventsFromLocalNode) {
        final EntryListener<K, V> listener = new EntryListener<K, V>() {
            @Override
            public void mapEvicted(MapEvent event) {
                if (includeEventsFromLocalNode || !event.getMember().localMember()) {
                    final NodeID eventNodeId = ClusteredCacheFactory.getNodeID(event.getMember());
                    logger.trace("Processing map evicted event of node '{}' for key '{}'", eventNodeId, event.getKey());
                    clusteredCacheEntryListener.mapEvicted(eventNodeId);
                }
            }

            @Override
            public void mapCleared(MapEvent event) {
                if (includeEventsFromLocalNode || !event.getMember().localMember()) {
                    final NodeID eventNodeId = ClusteredCacheFactory.getNodeID(event.getMember());
                    logger.trace("Processing map cleared event of node '{}'", eventNodeId);
                    clusteredCacheEntryListener.mapCleared(eventNodeId);
                }
            }

            @Override
            public void entryUpdated(EntryEvent event) {
                if (includeEventsFromLocalNode || !event.getMember().localMember()) {
                    final NodeID eventNodeId = ClusteredCacheFactory.getNodeID(event.getMember());
                    logger.trace("Processing entry update event of node '{}' for key '{}'", eventNodeId, event.getKey());
                    clusteredCacheEntryListener.entryUpdated((K) event.getKey(), (V) event.getOldValue(), (V) event.getValue(), eventNodeId);
                }
            }

            @Override
            public void entryRemoved(EntryEvent event) {
                if (includeEventsFromLocalNode || !event.getMember().localMember()) {
                    final NodeID eventNodeId = ClusteredCacheFactory.getNodeID(event.getMember());
                    logger.trace("Processing entry removed event of node '{}' for key '{}'", eventNodeId, event.getKey());
                    clusteredCacheEntryListener.entryRemoved((K) event.getKey(), (V) event.getOldValue(), eventNodeId);
                }
            }

            @Override
            public void entryEvicted(EntryEvent event) {
                if (includeEventsFromLocalNode || !event.getMember().localMember()) {
                    final NodeID eventNodeId = ClusteredCacheFactory.getNodeID(event.getMember());
                    logger.trace("Processing entry evicted event of node '{}' for key '{}'", eventNodeId, event.getKey());
                    clusteredCacheEntryListener.entryEvicted((K) event.getKey(), (V) event.getOldValue(), eventNodeId);
                }
            }

            @Override
            public void entryAdded(EntryEvent event) {
                if (includeEventsFromLocalNode || !event.getMember().localMember()) {
                    final NodeID eventNodeId = ClusteredCacheFactory.getNodeID(event.getMember());
                    logger.trace("Processing entry added event of node '{}' for key '{}'", eventNodeId, event.getKey());
                    clusteredCacheEntryListener.entryAdded((K) event.getKey(), (V) event.getValue(), eventNodeId);
                }
            }
        };

        final String listenerId = map.addEntryListener(listener, includeValues);
        listeners.add(listenerId);
        logger.debug("Added new clustered cache entry listener (including values: {}, includeEventsFromLocalNode: {}) using ID: '{}'", includeValues, includeEventsFromLocalNode, listenerId);
        return listenerId;
    }

    public void removeClusteredCacheEntryListener(@Nonnull final String listenerId) {
        logger.debug("Removing clustered cache entry listener: '{}'", listenerId);
        map.removeEntryListener(listenerId);
        listeners.remove(listenerId);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public V put(final K key, final V object) {
        if (object == null) { return null; }
        checkForPluginClassLoader(key);
        checkForPluginClassLoader(object);
        return map.put(key, object);
    }

    public V get(final Object key) {
        numberOfGets++;
        return map.get(key);
    }

    public V remove(final Object key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public int size() {
        final LocalMapStats stats = map.getLocalMapStats();
        return (int) (stats.getOwnedEntryCount() + stats.getBackupEntryCount());
    }

    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(final Object value) {
        return map.containsValue(value);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public void putAll(final Map<? extends K, ? extends V> entries) {
        map.putAll(entries);

        entries.entrySet().stream().findAny().ifPresent(
            e -> {
                checkForPluginClassLoader(e.getKey());
                checkForPluginClassLoader(e.getValue());
            }
        );
    }

    public Collection<V> values() {
        return map.values();
    }

    public long getCacheHits() {
        return map.getLocalMapStats().getHits();
    }

    public long getCacheMisses() {
        final long hits = map.getLocalMapStats().getHits();
        return numberOfGets > hits ? numberOfGets - hits : 0;
    }

    public int getCacheSize() {
        return (int) getLongCacheSize();
    }

    public long getLongCacheSize() {
        final LocalMapStats stats = map.getLocalMapStats();
        return stats.getOwnedEntryMemoryCost() + stats.getBackupEntryMemoryCost();
    }

    public long getMaxCacheSize() {
        return CacheFactory.getMaxCacheSize(getName());
    }

    public void setMaxCacheSize(int i) {
        setMaxCacheSize((long) i);
    }

    public void setMaxCacheSize(final long maxSize) {
        CacheFactory.setMaxSizeProperty(getName(), maxSize);
    }

    public long getMaxLifetime() {
        return CacheFactory.getMaxCacheLifetime(getName());
    }

    public void setMaxLifetime(final long maxLifetime) {
        CacheFactory.setMaxLifetimeProperty(getName(), maxLifetime);
    }

    void destroy() {
        listeners.forEach(map::removeEntryListener);
        map.destroy();
    }

    boolean lock(final K key, final long timeout) {
        boolean result = true;
        if (timeout < 0) {
            map.lock(key);
        } else if (timeout == 0) {
            result = map.tryLock(key);
        } else {
            try {
                result = map.tryLock(key, timeout, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException e) {
                logger.error("Failed to get cluster lock", e);
                result = false;
            }
        }
        return result;
    }

    void unlock(final K key) {
        try {
            map.unlock(key);
        } catch (final IllegalMonitorStateException e) {
            logger.error("Failed to release cluster lock", e);
        }
    }

    protected void checkForPluginClassLoader(final Object o) {
        if (o != null && o.getClass().getClassLoader() instanceof PluginClassLoader
            && lastPluginClassLoaderWarning.isBefore(Instant.now().minus(pluginClassLoaderWarningSupression)) )
        {
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
            logger.warn("An instance of {} that is loaded by {} has been added to the cache. " +
                "This will cause issues when reloading the plugin that provides this class. The plugin implementation should be modified.",
                o.getClass(), pluginName != null ? pluginName : "a PluginClassLoader");
            lastPluginClassLoaderWarning = Instant.now();
        }
    }
}