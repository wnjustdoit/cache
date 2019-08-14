package com.caiya.cache.redis.springx;

import com.caiya.serialization.jdk.JdkSerializationSerializer;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.NullValue;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * {@link org.springframework.cache.Cache} implementation using for Redis as underlying store.
 * <p/>
 * Use {@link JedisCacheManager} to create {@link JedisSpringCache} instances.
 *
 * @author wangnan
 * @see JedisCacheConfiguration
 * @see JedisCacheWriter
 * @since 1.1.1
 */
public class JedisSpringCache extends AbstractValueAdaptingCache {

    private static final byte[] BINARY_NULL_VALUE = new JdkSerializationSerializer().serialize(NullValue.INSTANCE);

    private final String name;
    private final JedisCacheWriter cacheWriter;
    private final JedisCacheConfiguration cacheConfig;

    /**
     * Create new {@link JedisSpringCache}.
     *
     * @param name        must not be {@literal null}.
     * @param cacheWriter must not be {@literal null}.
     * @param cacheConfig must not be {@literal null}.
     */
    protected JedisSpringCache(String name, JedisCacheWriter cacheWriter, JedisCacheConfiguration cacheConfig) {

        super(cacheConfig.getAllowCacheNullValues());

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(cacheWriter, "CacheWriter must not be null!");
        Assert.notNull(cacheConfig, "CacheConfig must not be null!");

        this.name = name;
        this.cacheWriter = cacheWriter;
        this.cacheConfig = cacheConfig;
    }

    @Override
    protected Object lookup(Object key) {

        byte[] value = cacheWriter.get(name, createAndConvertCacheKey(key));

        if (value == null) {
            return null;
        }

        return deserializeCacheValue(value);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public JedisCacheWriter getNativeCache() {
        return this.cacheWriter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T get(Object key, Callable<T> valueLoader) {

        ValueWrapper result = get(key);

        if (result != null) {
            return (T) result.get();
        }

        T value = valueFromLoader(key, valueLoader);
        put(key, value);
        return value;
    }

    @Override
    public void put(Object key, Object value) {

        Object cacheValue = preProcessCacheValue(value);

        if (!isAllowNullValues() && cacheValue == null) {

            throw new IllegalArgumentException(String.format(
                    "Cache '%s' does not allow 'null' values. Avoid storing null via '@Cacheable(unless=\"#result == null\")' or configure RedisCache to allow 'null' via RedisCacheConfiguration.",
                    name));
        }

        cacheWriter.put(name, createAndConvertCacheKey(key), serializeCacheValue(cacheValue), cacheConfig.getTtl());
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {

        Object cacheValue = preProcessCacheValue(value);

        if (!isAllowNullValues() && cacheValue == null) {
            return get(key);
        }

        byte[] result = cacheWriter.putIfAbsent(name, createAndConvertCacheKey(key), serializeCacheValue(cacheValue),
                cacheConfig.getTtl());

        if (result == null) {
            return null;
        }

        return new SimpleValueWrapper(fromStoreValue(deserializeCacheValue(result)));
    }

    @Override
    public void evict(Object key) {
        cacheWriter.remove(name, createAndConvertCacheKey(key));
    }

    @Override
    public void clear() {
        cacheWriter.clean(name, "*".getBytes());
    }


    private byte[] createAndConvertCacheKey(Object key) {
        return serializeCacheKey(createCacheKey(key));
    }

    /**
     * Serialize the key.
     *
     * @param cacheKey must not be {@literal null}.
     * @return never {@literal null}.
     */
    protected byte[] serializeCacheKey(String cacheKey) {
        return cacheConfig.getkeySerializer().serialize(cacheKey);
    }

    /**
     * Serialize the value to cache.
     *
     * @param value must not be {@literal null}.
     * @return never {@literal null}.
     */
    protected byte[] serializeCacheValue(Object value) {

        if (isAllowNullValues() && value instanceof NullValue) {
            return BINARY_NULL_VALUE;
        }

        return cacheConfig.getvalueSerializer().serialize(value);
    }

    /**
     * Customization hook for creating cache key before it gets serialized.
     *
     * @param key will never be {@literal null}.
     * @return never {@literal null}.
     */
    protected String createCacheKey(Object key) {

        String convertedKey = convertKey(key);

        if (!cacheConfig.usePrefix()) {
            return convertedKey;
        }

        return prefixCacheKey(convertedKey);
    }

    /**
     * Convert {@code key} to a {@link String} representation used for cache key creation.
     *
     * @param key will never be {@literal null}.
     * @return never {@literal null}.
     * @throws IllegalStateException if {@code key} cannot be converted to {@link String}.
     */
    protected String convertKey(Object key) {

        TypeDescriptor source = TypeDescriptor.forObject(key);

        Method toString = ReflectionUtils.findMethod(key.getClass(), "toString");

        if (toString != null && !Object.class.equals(toString.getDeclaringClass())) {
            return key.toString();
        }

        throw new IllegalStateException(
                String.format("Cannot convert %s to String. Register a Converter or override toString().", source));
    }

    private String prefixCacheKey(String key) {

        // allow contextual cache names by computing the key prefix on every call.
        return cacheConfig.getKeyPrefixFor(name) + key;
    }

    /**
     * Deserialize the given value to the actual cache value.
     *
     * @param value must not be {@literal null}.
     * @return can be {@literal null}.
     */
    protected Object deserializeCacheValue(byte[] value) {

        if (isAllowNullValues() && ObjectUtils.nullSafeEquals(value, BINARY_NULL_VALUE)) {
            return NullValue.INSTANCE;
        }

        return cacheConfig.getvalueSerializer().deserialize(value);
    }

    private static <T> T valueFromLoader(Object key, Callable<T> valueLoader) {

        try {
            return valueLoader.call();
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    /**
     * Customization hook called before passing object to
     * {@link com.caiya.serialization.Serializer}.
     *
     * @param value can be {@literal null}.
     * @return preprocessed value. Can be {@literal null}.
     */
    protected Object preProcessCacheValue(Object value) {

        if (value != null) {
            return value;
        }

        return isAllowNullValues() ? NullValue.INSTANCE : null;
    }
}
