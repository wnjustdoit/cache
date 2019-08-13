package com.caiya.cache.redis.spring;

import org.springframework.data.redis.cache.*;

/**
 * Extended Cache Client of Spring-data-redis' {@link org.springframework.data.redis.cache.RedisCache}.
 *
 * @author wangnan
 * @since 1.0
 */
public class ExtendedRedisCache extends org.springframework.data.redis.cache.RedisCache {
    /**
     * Create new {@link RedisCache}.
     *
     * @param name        must not be {@literal null}.
     * @param cacheWriter must not be {@literal null}.
     * @param cacheConfig must not be {@literal null}.
     */
    protected ExtendedRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig) {
        super(name, cacheWriter, cacheConfig);
    }
}