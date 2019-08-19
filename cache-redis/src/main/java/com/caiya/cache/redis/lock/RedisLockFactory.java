package com.caiya.cache.redis.lock;

import com.caiya.cache.CacheApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

/**
 * RedisLock Factory.
 */
public class RedisLockFactory {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockFactory.class);

    /**
     * The cache client
     */
    private CacheApi<String, String> cache;


    private RedisLockFactory() {
    }


    public static RedisLockFactory create(CacheApi<String, String> cache) {
        return new RedisLockFactory().setCache(cache);
    }

    public RedisLock buildLock(String name) {
        return new RedisLock(cache, name);
    }


    private RedisLockFactory setCache(CacheApi<String, String> cache) {
        this.cache = cache;
        return this;
    }

    /**
     * The destroy method
     */
    public void destroy() {
        if (cache instanceof Closeable) {
            try {
                ((Closeable) cache).close();
            } catch (IOException e) {
                logger.error("cache client close failed", e);
            }
        }
    }
}
