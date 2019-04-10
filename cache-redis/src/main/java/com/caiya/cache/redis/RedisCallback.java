package com.caiya.cache.redis;

/**
 * Callback interface for Redis 'low level' code. To be used with {@link org.springframework.data.redis.core.RedisTemplate} execution methods, often as
 * anonymous classes within a method implementation. Usually, used for chaining several operations together (
 * {@code get/set/trim etc...}.
 *
 * @param <R> type of result
 * @param <K> type of key
 * @param <V> type of value
 * @author wangnan
 * @since 1.0
 */
public interface RedisCallback<R, K, V> {

    R doInRedis(JedisCache<K, V> jedisCache);

}
