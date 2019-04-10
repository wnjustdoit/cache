package com.caiya.cache.redis;

import com.caiya.serialization.Serializer;
import com.caiya.serialization.jdk.StringSerializer;

import java.util.Arrays;

/**
 * Redis Key Wrapper Class.
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisCacheKey {

    private final Object keyElement;
    private byte[] prefix;
    @SuppressWarnings("rawtypes")
    private Serializer serializer;

    private static final Serializer<?> DEFAULT_SERIALIZER = new StringSerializer();

    /**
     * @param keyElement must not be {@literal null}.
     */
    public RedisCacheKey(Object keyElement) {
        if (keyElement == null)
            throw new IllegalArgumentException("KeyElement must not be null!");

        this.keyElement = keyElement;
    }

    /**
     * Get the {@link Byte} representation of the given key element using prefix if available.
     */
    public byte[] getKeyBytes() {
        byte[] rawKey = serializeKeyElement();
        if (!hasPrefix()) {
            return rawKey;
        }

        byte[] prefixedKey = Arrays.copyOf(prefix, prefix.length + rawKey.length);
        System.arraycopy(rawKey, 0, prefixedKey, prefix.length, rawKey.length);

        return prefixedKey;
    }

    /**
     * @return the original key
     */
    public Object getKeyElement() {
        return keyElement;
    }

    @SuppressWarnings("unchecked")
    private byte[] serializeKeyElement() {
        if (serializer == null) {
            if (keyElement instanceof byte[]) {
                return (byte[]) keyElement;
            }
            serializer = DEFAULT_SERIALIZER;
        }

        return serializer.serialize(keyElement);
    }

    /**
     * Set the {@link Serializer} used for converting the key into its {@link Byte} representation.
     *
     * @param serializer can be {@literal null}.
     */
    public void setSerializer(Serializer<?> serializer) {
        this.serializer = serializer;
    }

    /**
     * @return true if prefix is not empty.
     */
    public boolean hasPrefix() {
        return (prefix != null && prefix.length > 0);
    }

    /**
     * Use the given prefix when generating key.
     *
     * @param prefix can be {@literal null}.
     * @return this object
     */
    public RedisCacheKey usePrefix(byte[] prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Use {@link Serializer} for converting the key into its {@link Byte} representation.
     *
     * @param serializer can be {@literal null}.
     * @return this object
     */
    public RedisCacheKey withKeySerializer(Serializer serializer) {
        this.serializer = serializer;
        return this;
    }

}
