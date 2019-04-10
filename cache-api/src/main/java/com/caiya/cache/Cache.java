package com.caiya.cache;

import java.io.Closeable;
import java.io.IOException;

/**
 * Cache Client Interface.
 */
public interface Cache<K, V> extends CacheApi<K, V>, Closeable {

    /**
     * Return the cache name.
     */
    String getName();

    /**
     * Close the Client.
     * Remember do this after using the client up!!(except for RedisCache based on spring-data-redis)
     */
    @Override
    void close() throws IOException;

}
