package com.redislabs.redisgraph.impl.api;

import com.redislabs.redisgraph.RedisGraph;
import com.redislabs.redisgraph.ResultSet;
import com.redislabs.redisgraph.impl.Utils;
import com.redislabs.redisgraph.impl.graph_cache.RedisGraphCaches;
import com.redislabs.redisgraph.impl.resultset.ResultSetImpl;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.commands.PipelinedCommand;
import redis.clients.jedis.params.QueryParams;
import redis.clients.jedis.util.SafeEncoder;

import java.util.List;
import java.util.Map;

/**
 * This class is extending Jedis Pipeline
 */
public class RedisGraphPipeline extends Pipeline implements com.redislabs.redisgraph.RedisGraphPipeline, RedisGraphCacheHolder {

    private final RedisGraph redisGraph;
    private RedisGraphCaches caches;

    public RedisGraphPipeline(RedisGraph redisGraph){
        this.redisGraph = redisGraph;
    }

    /**
     * Execute a Cypher query.
     * @param graphId a graph to perform the query on
     * @param query Cypher query
     * @return  a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> query(String graphId, String query) {
        return query(graphId, query, null, 0);
    }

    /**
     * Execute a Cypher read-oly query.
     * @param graphId a graph to perform the query on
     * @param query Cypher query
     * @return  a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> readOnlyQuery(String graphId, String query) {
        return readOnlyQuery(graphId, query, null, 0);
    }

    /**
     * Executes a cypher query with parameters and timeout.
     *
     * NOTE: timeout is simply sent to DB. Socket timeout will not be changed.
     * timeout.
     * @param graphId a graph to perform the query on.
     * @param query Cypher query.
     * @param params parameters map.
     * @param timeout
     * @return  a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> query(String graphId, String query, Map<String, Object> params, long timeout) {
        String preparedQuery = Utils.prepareQuery(query, params);
        Response<ResultSet> response = new Response<>();
        response.set(new PipelinedCommand<>(response, this, new Builder<ResultSet>() {
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        }, SafeEncoder.encode(graphId), SafeEncoder.encode(preparedQuery), SafeEncoder.encode(Utils.COMPACT_STRING), SafeEncoder.encode(Utils.TIMEOUT_STRING), SafeEncoder.encode(Long.toString(timeout))));
        return response;
    }

    /**
     * Executes a cypher read-only query with parameters and timeout.
     *
     * NOTE: timeout is simply sent to DB. Socket timeout will not be changed.
     * timeout.
     * @param graphId a graph to perform the query on.
     * @param query Cypher query.
     * @param params parameters map.
     * @param timeout
     * @return  a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> readOnlyQuery(String graphId, String query, Map<String, Object> params, long timeout) {
        String preparedQuery = Utils.prepareQuery(query, params);
        Response<ResultSet> response = new Response<>();
        response.set(new PipelinedCommand<>(response, this, new Builder<ResultSet>() {
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        }, SafeEncoder.encode(graphId), SafeEncoder.encode(preparedQuery), SafeEncoder.encode(Utils.COMPACT_STRING), SafeEncoder.encode(Utils.TIMEOUT_STRING), SafeEncoder.encode(Long.toString(timeout))));
        return response;
    }

    /**
     * Invokes stored procedures without arguments
     * @param graphId a graph to perform the query on
     * @param procedure procedure name to invoke
     * @return response with result set with the procedure data
     */
    public Response<ResultSet> callProcedure(String graphId, String procedure){
        return callProcedure(graphId, procedure, Utils.DUMMY_LIST, Utils.DUMMY_MAP);
    }

    /**
     * Invokes stored procedure with arguments
     * @param graphId a graph to perform the query on
     * @param procedure procedure name to invoke
     * @param args procedure arguments
     * @return response with result set with the procedure data
     */
    public Response<ResultSet> callProcedure(String graphId, String procedure, List<String> args  ){
        return callProcedure(graphId, procedure, args, Utils.DUMMY_MAP);
    }

    /**
     * Invoke a stored procedure
     * @param graphId a graph to perform the query on
     * @param procedure - procedure to execute
     * @param args - procedure arguments
     * @param kwargs - procedure output arguments
     * @return response with result set with the procedure data
     */
    public Response<ResultSet> callProcedure(String graphId, String procedure, List<String> args,
                                                  Map<String, List<String>> kwargs) {
        String preparedProcedure = Utils.prepareProcedure(procedure, args, kwargs);
        Response<ResultSet> response = new Response<>();
        response.set(new PipelinedCommand<>(response, this, new Builder<ResultSet>() {
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        }, SafeEncoder.encode(graphId), SafeEncoder.encode(preparedProcedure)));
        return response;
    }

    /**
     * Deletes the entire graph
     * @param graphId graph to delete
     * @return response with the deletion running time statistics
     */
    public Response<String> deleteGraph(String graphId){
        Response<String> response = new Response<>();
        response.set(new PipelinedCommand<>(response, this, BuilderFactory.STRING, SafeEncoder.encode(RedisGraphCommand.DELETE), SafeEncoder.encode(graphId)));
        caches.removeGraphCache(graphId);
        return response;
    }

    @Override
    public void setRedisGraphCaches(RedisGraphCaches caches) {
        this.caches = caches;
    }
}