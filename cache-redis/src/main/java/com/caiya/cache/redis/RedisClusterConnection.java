package com.caiya.cache.redis;

/**
 * {@link RedisClusterConnection} allows sending commands to dedicated nodes within the cluster.
 *
 * @param <T> client type
 * @author wangnan
 * @since 1.0
 */
public interface RedisClusterConnection<T> extends RedisConnection<T> {

}
