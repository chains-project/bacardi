package com.redislabs.redisgraph.impl.api;

import java.util.List;

import com.redislabs.redisgraph.RedisGraphContext;
import com.redislabs.redisgraph.ResultSet;
import com.redislabs.redisgraph.exceptions.JRedisGraphException;
import com.redislabs.redisgraph.impl.Utils;
import com.redislabs.redisgraph.impl.graph_cache.RedisGraphCaches;
import com.redislabs.redisgraph.impl.resultset.ResultSetImpl;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.RedisGraphCommand;
import redis.clients.jedis.util.SafeEncoder;

/**
 * An implementation of RedisGraphContext. Allows sending RedisGraph and some Redis commands,
 * within a specific connection context
 */
public class ContextedRedisGraph extends AbstractRedisGraph implements RedisGraphContext, RedisGraphCacheHolder {

{
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
    protected Jedis getConnection() {
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
        } catch (Exception e) {
            conn.close();
            throw e;
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
        } catch (Exception e) {
            conn.close();
            throw e;
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
        try {
            @SuppressWarnings("unchecked")
            List<Object> rawResponse = (List<Object>) conn.sendBlockingCommand(RedisGraphCommand.QUERY, graphId, preparedQuery, Utils.COMPACT_STRING, Utils.TIMEOUT_STRING, Long.toString(timeout));
            return new ResultSetImpl(rawResponse, this, caches.getGraphCache(graphId));
        } catch (Exception e) {
            conn.close();
            throw e;
        }
    }

    /**
     * Sends the read-only query over the instance only connection
     * @param graphId graph to be queried
     * @param timeout
     * @param preparedQuery prepared query
     * @return Result set with the query answer
     */
    @Override
    protected ResultSet sendReadOnlyQuery(String graphId, String preparedQuery, long timeout) {
        Jedis conn = getConnection();
        try {
            @SuppressWarnings("unchecked")
            List<Object> rawResponse = (List<Object>) conn.sendBlockingCommand(RedisGraphCommand.RO_QUERY, graphId, preparedQuery, Utils.COMPACT_STRING, Utils.TIMEOUT_STRING, Long.toString(timeout));
            return new ResultSetImpl(rawResponse, this, caches.getGraphCache(graphId));
        } catch (Exception e) {
            conn.close();
            throw e;
        }
    }

    /**
     * @return Returns the instance Jedis connection.
     */
    @Override
    public Jedis getConnectionContext() {
        return this.connectionContext;
    }

    /**
     * Creates a new RedisGraphTransaction transactional object
     * @return new RedisGraphTransaction
     */
    @Override
    public RedisGraphTransaction multi() {
        Jedis jedis = getConnection();
        Transaction transaction = jedis.multi();
        RedisGraphTransaction redisGraphTransaction = new RedisGraphTransaction(transaction, this);
        redisGraphTransaction.setRedisGraphCaches(caches);
        return redisGraphTransaction;
    }

    /**
     * Creates a new RedisGraphPipeline pipeline object
     * @return new RedisGraphPipeline
     */
    @Override
    public RedisGraphPipeline pipelined() {
        Jedis jedis = getConnection();
        Pipeline pipeline = jedis.pipelined();
        RedisGraphPipeline redisGraphPipeline = new RedisGraphPipeline(pipeline, this);
        redisGraphPipeline.setRedisGraphCaches(caches);
        return redisGraphPipeline;
    }

    /**
     * Perfrom watch over given Redis keys
     * @param keys
     * @return "OK"
     */
    @Override
    public String watch(String... keys) {
        return this.getConnection().watch(keys);
    }

    /**
     * Removes watch from all keys
     * @return
     */
    @Override
    public String unwatch() {
        return this.getConnection().unwatch();
    }

    /**
     * Deletes the entire graph
     * @param graphId graph to be deleted
     * @return delete running time statistics
     */
    @Override
    public String deleteGraph(String graphId) {
        Jedis conn = getConnection();
        try {
            Object response = conn.sendCommand(RedisGraphCommand.DELETE, graphId);
            return SafeEncoder.encode((byte[]) response);
        } catch (Exception e) {
            conn.close();
            throw e;
        }
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
}