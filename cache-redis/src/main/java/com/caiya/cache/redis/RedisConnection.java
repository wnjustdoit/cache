package com.caiya.cache.redis;

import java.io.Closeable;
import java.io.IOException;

/**
 * A connection to a Redis server.
 *
 * @param <T> client type
 * @author wangnan
 * @since 1.0
 */
public interface RedisConnection<T> extends Closeable {

    /**
     * Closes (or quits) the connection.
     *
     * @throws IOException
     */
    @Override
    void close() throws IOException;

    /**
     * Indicates whether the underlying connection is closed or not.
     *
     * @return true if the connection is closed, false otherwise.
     */
    boolean isClosed();

    /**
     * Returns the native connection (the underlying library/driver object).
     *
     * @return underlying, native object
     */
    T getNativeConnection();


}
