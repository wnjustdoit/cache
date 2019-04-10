package com.caiya.cache.redis;

import java.io.IOException;

/**
 * {@link RedisConnection} implementation on top of {@link JedisCache}.<br/>
 *
 * @author wangnan
 * @since 1.0
 */
public class JedisClusterConnection implements RedisClusterConnection<JedisCache> {

    private final JedisCache<?, ?> jedisCache;

    private boolean closed;

    public JedisClusterConnection(JedisCache<?, ?> jedisCache) {
        this.jedisCache = jedisCache;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            // do nothing..
            closed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public JedisCache<?, ?> getNativeConnection() {
        return jedisCache;
    }
}
