package com.caiya.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link RedisTemplate} defining common properties. Not intended to be used directly.
 *
 * @author wangnan
 * @since 1.0
 */
public abstract class RedisAccessor<K, V> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private RedisConnectionFactory connectionFactory;

    /**
     * Returns the connectionFactory.
     *
     * @return Returns the connectionFactory
     */
    public RedisConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Sets the connection factory.
     *
     * @param connectionFactory The connectionFactory to set.
     */
    public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @SuppressWarnings("unchecked")
    protected void afterPropertiesSet() {
        if (getConnectionFactory() == null)
            throw new IllegalArgumentException("RedisConnectionFactory is required");

    }

}
