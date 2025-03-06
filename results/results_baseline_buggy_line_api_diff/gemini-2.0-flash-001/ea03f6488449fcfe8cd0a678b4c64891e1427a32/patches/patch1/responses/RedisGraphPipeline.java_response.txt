package com.redislabs.redisgraph.impl.api;

import com.redislabs.redisgraph.RedisGraph;
import com.redislabs.redisgraph.ResultSet;
import com.redislabs.redisgraph.impl.Utils;
import com.redislabs.redisgraph.impl.graph_cache.RedisGraphCaches;
import com.redislabs.redisgraph.impl.resultset.ResultSetImpl;
import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.CommandArguments;

import java.util.List;
import java.util.Map;

/**
 * This class is extending Jedis Pipeline
 */
public class RedisGraphPipeline extends Pipeline implements com.redislabs.redisgraph.RedisGraphPipeline, RedisGraphCacheHolder {

    private final RedisGraph redisGraph;
    private RedisGraphCaches caches;
    private final Jedis jedis;


    public RedisGraphPipeline(Jedis jedis, RedisGraph redisGraph){
        super(jedis);
        this.jedis = jedis;
        this.redisGraph = redisGraph;
    }

    /**
     * Execute a Cypher query.
     * @param graphId a graph to perform the query on
     * @param query Cypher query
     * @return a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> query(String graphId, String query) {
        CommandArguments args = new CommandArguments().add(graphId).add(query).add(Utils.COMPACT_STRING);
        sendCommand(RedisGraphCommand.QUERY, args);
        return getResponse(new Builder<ResultSet>() {
            @SuppressWarnings("unchecked")
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        });
    }

    /**
     * Execute a Cypher read-oly query.
     * @param graphId a graph to perform the query on
     * @param query Cypher query
     * @return a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> readOnlyQuery(String graphId, String query) {
        CommandArguments args = new CommandArguments().add(graphId).add(query).add(Utils.COMPACT_STRING);
        sendCommand(RedisGraphCommand.RO_QUERY, args);
        return getResponse(new Builder<ResultSet>() {
            @SuppressWarnings("unchecked")
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        });
    }

    /**
     * Execute a Cypher query with timeout.
     *
     * NOTE: timeout is simply sent to DB. Socket timeout will not be changed.
     * @param graphId a graph to perform the query on
     * @param query Cypher query
     * @param timeout
     * @return a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> query(String graphId, String query, long timeout) {
        CommandArguments args = new CommandArguments().add(graphId).add(query).add(Utils.COMPACT_STRING).add(Utils.TIMEOUT_STRING).add(Long.toString(timeout));
        sendCommand(RedisGraphCommand.QUERY, args);
        return getResponse(new Builder<ResultSet>() {
            @SuppressWarnings("unchecked")
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        });
    }

    /**
     * Execute a Cypher read-only query with timeout.
     *
     * NOTE: timeout is simply sent to DB. Socket timeout will not be changed.
     * @param graphId a graph to perform the query on
     * @param query Cypher query
     * @param timeout
     * @return a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> readOnlyQuery(String graphId, String query, long timeout) {
        CommandArguments args = new CommandArguments().add(graphId).add(query).add(Utils.COMPACT_STRING).add(Utils.TIMEOUT_STRING).add(Long.toString(timeout));
        sendCommand(RedisGraphCommand.RO_QUERY, args);
        return getResponse(new Builder<ResultSet>() {
            @SuppressWarnings("unchecked")
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        });
    }

    /**
     * Executes a cypher query with parameters.
     * @param graphId a graph to perform the query on.
     * @param query Cypher query.
     * @param params parameters map.
     * @return  a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> query(String graphId, String query, Map<String, Object> params) {
        String preparedQuery = Utils.prepareQuery(query, params);
        CommandArguments args = new CommandArguments().add(graphId).add(preparedQuery).add(Utils.COMPACT_STRING);
        sendCommand(RedisGraphCommand.QUERY, args);
        return getResponse(new Builder<ResultSet>() {
            @SuppressWarnings("unchecked")
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        });
    }

    /**
     * Executes a cypher read-only query with parameters.
     * @param graphId a graph to perform the query on.
     * @param query Cypher query.
     * @param params parameters map.
     * @return  a response which builds the result set with the query answer.
     */
    @Override
    public Response<ResultSet> readOnlyQuery(String graphId, String query, Map<String, Object> params) {
        String preparedQuery = Utils.prepareQuery(query, params);
        CommandArguments args = new CommandArguments().add(graphId).add(preparedQuery).add(Utils.COMPACT_STRING);
        sendCommand(RedisGraphCommand.RO_QUERY, args);
        return getResponse(new Builder<ResultSet>() {
            @SuppressWarnings("unchecked")
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        });
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
        CommandArguments args = new CommandArguments().add(graphId).add(preparedQuery).add(Utils.COMPACT_STRING).add(Utils.TIMEOUT_STRING).add(Long.toString(timeout));
        sendCommand(RedisGraphCommand.QUERY, args);
        return getResponse(new Builder<ResultSet>() {
            @SuppressWarnings("unchecked")
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        });
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
        CommandArguments args = new CommandArguments().add(graphId).add(preparedQuery).add(Utils.COMPACT_STRING).add(Utils.TIMEOUT_STRING).add(Long.toString(timeout));
        sendCommand(RedisGraphCommand.RO_QUERY, args);
        return getResponse(new Builder<ResultSet>() {
            @SuppressWarnings("unchecked")
            @Override
            public ResultSet build(Object o) {
                return new ResultSetImpl((List<Object>) o, redisGraph, caches.getGraphCache(graphId));
            }
        });
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
        return query(graphId, preparedProcedure);
    }


    /**
     * Deletes the entire graph
     * @param graphId graph to delete
     * @return response with the deletion running time statistics
     */
    public Response<String> deleteGraph(String graphId){
        CommandArguments args = new CommandArguments().add(graphId);
        sendCommand(RedisGraphCommand.DELETE, args);
        Response<String> response =  getResponse(BuilderFactory.STRING);
        caches.removeGraphCache(graphId);
        return response;
    }

    @Override
    public void setRedisGraphCaches(RedisGraphCaches caches) {
        this.caches = caches;
    }

    private void sendCommand(ProtocolCommand cmd, CommandArguments args) {
        jedis.sendCommand(cmd, args);
    }

    private <T> Response<T> getResponse(Builder<T> builder) {
        return super.getResponse(builder);
    }
}