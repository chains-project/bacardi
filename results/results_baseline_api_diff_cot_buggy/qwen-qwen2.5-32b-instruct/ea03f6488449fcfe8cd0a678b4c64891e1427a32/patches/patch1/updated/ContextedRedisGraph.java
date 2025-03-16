package com.redislabs.redisgraph.impl.api;

import java.util.List;

import com.redislabs.redisgraph.RedisGraphContext;
import com.redislabs.redisgraph.ResultSet;
import com.redislabs.redisgraph.exceptions.JRedisGraphException;
import com.redislabs.redisgraph.impl.Utils;
import com.redislabs.redisgraph.impl.graph_cache.RedisGraphCaches;
import com.redislabs.redisgraph.impl.resultset.ResultSetImpl;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

/**
 * An implementation of RedisGraphContext. Allows sending RedisGraph and some Redis commands,
 * within a specific connection context
 */
public class ContextedRedisGraph extends AbstractRedisGraph implements RedisGraphContext, RedisGraphCacheHolder {

    private final Jedis connectionContext;
    private RedisGraphCaches caches;

    /**
     * Generates a new instance with a specific Jedis connection
     * @param connectionContext
     */
    public ContextedRedisGraph(Jedis connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * Overrides the abstract method. Return the instance only connection
     * @return
     */
    @Override
    public Jedis getConnection() {
        return this.connectionContext;
    }

    /**
     * Sends the query over the instance only connection
     * @param graphId graph to be queried
     * @param preparedQuery prepared query
     * @return Result set with the query answer
     */
    @Override
    protected ResultSet sendQuery(String graphId, String preparedQuery) {
        Jedis conn = getConnection();
        try {
            @SuppressWarnings("unchecked")
            List<Object> rawResponse = (List<Object>) conn.sendCommand(RedisGraphCommand.QUERY, graphId, preparedQuery, Utils.COMPACT_STRING);
            return new ResultSetImpl(rawResponse, this, caches.getGraphCache(graphId));
        } catch (JRedisGraphException rt) {
            throw rt;
        } catch (JedisDataException j) {
            throw new JRedisGraphException(j);
        }
    }

    /**
     * Sends the read-only query over the instance only connection
     * @param graphId graph to be queried
     * @param preparedQuery prepared query
     * @return Result set with the query answer
     */
    @Override
    protected ResultSet sendReadOnlyQuery(String graphId, String preparedQuery) {
        Jedis conn = getConnection();
        try {
            @SuppressWarnings("unchecked")
            List<Object> rawResponse = (List<Object>) conn.sendCommand(RedisGraphCommand.RO_QUERY, graphId, preparedQuery, Utils.COMPACT_STRING);
            return new ResultSetImpl(rawResponse, this, caches.getGraphCache(graphId));
        } catch (JRedisGraphException ge) {
            throw ge;
        } catch (JedisDataException de) {
            throw new JRedisGraphException(de);
        }
    }

    /**
     * Sends the query over the instance only connection
     * @param graphId graph to be queried
     * @param timeout
     * @param preparedQuery prepared query
     * @return Result set with the query answer
     */
    @Override
    protected ResultSet sendQuery(String graphId, String preparedQuery, long timeout) {
        Jedis conn = getConnection();
        Object response;
        try {
            response = conn.sendCommand(RedisGraphCommand.QUERY, graphId, preparedQuery, Utils.COMPACT_STRING, Utils.TIMEOUT_STRING, Long.toString(timeout));
        } catch (Exception e) {
            conn.close();
            throw e;
        }
        //clear local state
        caches.removeGraphCache(graphId);
        return SafeEncoder.encode((byte[]) response);
    }

    /**
     * Deletes the entire graph
     * @param graphId graph to delete
     * @return delete running time statistics
     */
    @Override
    public String deleteGraph(String graphId) {
        Jedis conn = getConnection();
        Object response;
        try {
            response = conn.sendCommand(RedisGraphCommand.DELETE, graphId);
        } catch (Exception e) {
            conn.close();
            throw e;
        }
        //clear local state
        caches.removeGraphCache(graphId);
        return SafeEncoder.encode((byte[]) response);
    }

    /**
     * closes the Jedis connection
     */
    @Override
    public void close() {
        this.connectionContext.close();
    }

    @Override
    public void setRedisGraphCaches(RedisGraphCaches caches) {
        this.caches = caches;
    }

    /**
     * Creates a new RedisGraphTransaction transactional object
     * @return new RedisGraphTransaction
     */
    public RedisGraphTransaction multi() {
        Jedis jedis = getConnection();
        // Assuming Jedis now does not expose Client directly, and transaction handling is done differently
        // Placeholder for transaction handling logic
        RedisGraphTransaction transaction = new RedisGraphTransaction(jedis, this);
        transaction.setRedisGraphCaches(caches);
        return transaction;
    }

    /**
     * Creates a new RedisGraphPipeline pipeline object
     * @return new RedisGraphPipeline
     */
    public RedisGraphPipeline pipelined() {
        Jedis jedis = getConnection();
        // Assuming Jedis now does not expose Client directly, and pipeline handling is done differently
        // Placeholder for pipeline handling logic
        RedisGraphPipeline pipeline = new RedisGraphPipeline(jedis, this);
        pipeline.setRedisGraphCaches(caches);
        return pipeline;
    }

    /**
     * Perfrom watch over given Redis keys
     * @param keys
     * @return "OK"
     */
    public String watch(String... keys) {
        return this.getConnection().watch(keys);
    }

    /**
     * Removes watch from all keys
     * @return
     */
    public String unwatch() {
        return this.getConnection().unwatch();
    }
}
