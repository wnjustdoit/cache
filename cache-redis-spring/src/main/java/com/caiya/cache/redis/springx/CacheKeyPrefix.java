package com.caiya.cache.redis.springx;

/**
 * {@link CacheKeyPrefix} provides a hook for creating custom prefixes prepended to the actual {@literal key} stored in
 * Redis.
 *
 * @author wangnan
 * @since 1.1.1
 */
@FunctionalInterface
public interface CacheKeyPrefix {

    /**
     * Compute the prefix for the actual {@literal key} stored in Redis.
     *
     * @param cacheName will never be {@literal null}.
     * @return never {@literal null}.
     */
    String compute(String cacheName);

    /**
     * Creates a default {@link CacheKeyPrefix} scheme that prefixes cache keys with {@code cacheName} followed by double
     * colons. A cache named {@code myCache} will prefix all cache keys with {@code myCache::}.
     *
     * @return the default {@link CacheKeyPrefix} scheme.
     */
    static CacheKeyPrefix simple() {
        return name -> name + "::";
    }
}
