package com.caiya.cache.redis;

import com.caiya.cache.CacheApi;

/**
 * Interface that specified a basic set of Redis operations, implemented by {@link RedisTemplate}. Not often used but a
 * useful option for extensibility and testability (as it can be easily mocked or stubbed).
 *
 * @author wangnan
 * @since 1.0
 */
public interface RedisOperations<K, V> extends CacheApi<K, V> {

    <R> R execute(RedisCallback<R, K, V> action);

}
