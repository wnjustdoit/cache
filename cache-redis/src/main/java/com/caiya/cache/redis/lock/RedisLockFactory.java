package com.caiya.cache.redis.lock;

import com.caiya.cache.CacheApi;

import java.time.Duration;

/**
 * RedisLock Factory.
 */
public class RedisLockFactory {

    /**
     * The cache client
     */
    private CacheApi<String, String> cache;

    /**
     * The lock timeout
     */
    private Duration lockTimeout;


    private RedisLockFactory() {
    }


    public static RedisLockFactory create(CacheApi<String, String> cache) {
        return new RedisLockFactory().setCache(cache);
    }

    public RedisLockFactory withLockTimeout(Duration lockTimeout) {
        return setLockTimeout(lockTimeout);
    }

    public RedisLock buildLock(String name) {
        return new RedisLock(cache, name);
    }

    public RedisLock buildLock(String name, Duration lockTimeout) {
        return new RedisLock(cache, name, lockTimeout);
    }


    private RedisLockFactory setCache(CacheApi<String, String> cache) {
        this.cache = cache;
        return this;
    }

    private RedisLockFactory setLockTimeout(Duration lockTimeout) {
        if (lockTimeout == null) {
            throw new IllegalArgumentException("lock timeout cannot be null");
        }
        this.lockTimeout = lockTimeout;
        return this;
    }

    public Duration getLockTimeout() {
        return lockTimeout;
    }
}
