package com.caiya.cache.redis;

/**
 * Thread-safe factory of Redis connections.
 *
 * @author wangnan
 * @since 1.0
 */
public interface RedisConnectionFactory {

    /**
     * Provides a suitable connection for interacting with Redis.
     *
     * @return connection for interacting with Redis.
     */
    RedisConnection getConnection();

    /**
     * Provides a suitable connection for interacting with Redis Cluster.
     *
     * @return connection for interacting with Redis Cluster.
     */
    RedisClusterConnection getClusterConnection();

    /**
     * Init method.
     */
    void afterPropertiesSet();

    /**
     * Destroy method.
     */
    void destroy();

}
