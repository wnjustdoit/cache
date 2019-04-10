package com.caiya.cache.redis;

/**
 * Element to be stored inside {@link JedisCache}.
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisCacheElement {

    private final RedisCacheKey cacheKey;
    private final Object value;
    private long timeToLive;

    /**
     * @param cacheKey the key to be used for storing value in {@link JedisCache}. Must not be {@literal null}.
     * @param value    redis value
     */
    public RedisCacheElement(RedisCacheKey cacheKey, Object value) {
        if (cacheKey == null)
            throw new IllegalArgumentException("CacheKey must not be null!");

        this.cacheKey = cacheKey;
        this.value = value;
    }

    /**
     * Get the binary key representation.
     *
     * @return bytes of key
     */
    public byte[] getKeyBytes() {
        return cacheKey.getKeyBytes();
    }

    /**
     * @return RedisCacheKey
     */
    public RedisCacheKey getKey() {
        return cacheKey;
    }

    /**
     * Set the elements time to live. Use {@literal zero} to store eternally.
     *
     * @param timeToLive live time
     */
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * @return live time
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
     * @return true in case {@link RedisCacheKey} is prefixed.
     */
    public boolean hasKeyPrefix() {
        return cacheKey.hasPrefix();
    }

    /**
     * @return true if timeToLive is 0
     */
    public boolean isEternal() {
        return 0 == timeToLive;
    }

    /**
     * Expire the element after given seconds.
     *
     * @param seconds live time
     * @return this object
     */
    public RedisCacheElement expireAfter(long seconds) {

        setTimeToLive(seconds);
        return this;
    }
}
