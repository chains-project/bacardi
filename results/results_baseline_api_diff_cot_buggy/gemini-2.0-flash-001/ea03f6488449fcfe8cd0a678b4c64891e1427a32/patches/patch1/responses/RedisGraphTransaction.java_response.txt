public interface RedisGraphTransaction extends
        MultiKeyBinaryRedisPipeline,
        MultiKeyCommandsPipeline, ClusterPipeline,
        BinaryScriptingCommandsPipeline, ScriptingCommandsPipeline,
        BasicRedisPipeline, BinaryRedisPipeline, RedisPipeline, Closeable {