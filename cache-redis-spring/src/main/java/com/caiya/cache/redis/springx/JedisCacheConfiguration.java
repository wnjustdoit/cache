package com.caiya.cache.redis.springx;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import com.caiya.serialization.Serializer;
import com.caiya.serialization.jdk.JdkSerializationSerializer;
import com.caiya.serialization.jdk.StringSerializer;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;

/**
 * Immutable {@link JedisCacheConfiguration} helps customizing {@link JedisSpringCache} behaviour such as caching
 * {@literal null} values, cache key prefixes and binary serialization. <br />
 * Start with {@link JedisCacheConfiguration#defaultCacheConfig()} and customize {@link JedisSpringCache} behaviour from there
 * on.
 *
 * @author wangnan
 * @since 1.1.1
 */
public class JedisCacheConfiguration {

    private final Duration ttl;
    private final boolean cacheNullValues;
    private final CacheKeyPrefix keyPrefix;
    private final boolean usePrefix;

    private final Serializer<String> keySerializer;
    private final Serializer<Object> valueSerializer;

    public JedisCacheConfiguration(Duration ttl, boolean cacheNullValues, boolean usePrefix, CacheKeyPrefix keyPrefix,
                                   Serializer<String> keySerializer, Serializer<Object> valueSerializer) {
        this.ttl = ttl;
        this.cacheNullValues = cacheNullValues;
        this.usePrefix = usePrefix;
        this.keyPrefix = keyPrefix;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    /**
     * Default {@link JedisCacheConfiguration} using the following:
     * <dl>
     * <dt>key expiration</dt>
     * <dd>eternal</dd>
     * <dt>cache null values</dt>
     * <dd>yes</dd>
     * <dt>prefix cache keys</dt>
     * <dd>yes</dd>
     * <dt>default prefix</dt>
     * <dd>[the actual cache name]</dd>
     * <dt>key serializer</dt>
     * <dd>StringRedisSerializer.class</dd>
     * <dt>value serializer</dt>
     * <dd>JdkSerializationRedisSerializer.class</dd>
     * <dt>conversion service</dt>
     * <dd>{@link DefaultFormattingConversionService} with {@link #registerDefaultConverters(ConverterRegistry) default}
     * cache key converters</dd>
     * </dl>
     *
     * @return new {@link JedisCacheConfiguration}.
     */
    public static JedisCacheConfiguration defaultCacheConfig() {

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

        registerDefaultConverters(conversionService);

        return new JedisCacheConfiguration(Duration.ZERO, true, true, CacheKeyPrefix.simple(),
                new StringSerializer(),
                new JdkSerializationSerializer());
    }

    /**
     * Set the ttl to apply for cache entries. Use {@link Duration#ZERO} to declare an eternal cache.
     *
     * @param ttl must not be {@literal null}.
     * @return new {@link JedisCacheConfiguration}.
     */
    public JedisCacheConfiguration entryTtl(Duration ttl) {

        Assert.notNull(ttl, "TTL duration must not be null!");

        return new JedisCacheConfiguration(ttl, cacheNullValues, usePrefix, keyPrefix, keySerializer, valueSerializer);
    }

    /**
     * Use the given prefix instead of the default one.
     *
     * @param prefix must not be {@literal null}.
     * @return new {@link JedisCacheConfiguration}.
     */
    public JedisCacheConfiguration prefixKeysWith(String prefix) {

        Assert.notNull(prefix, "Prefix must not be null!");

        return computePrefixWith((cacheName) -> prefix);
    }

    /**
     * Use the given {@link CacheKeyPrefix} to compute the prefix for the actual Redis {@literal key} on the
     * {@literal cache name}.
     *
     * @param cacheKeyPrefix must not be {@literal null}.
     * @return new {@link JedisCacheConfiguration}.
     * @since 2.0.4
     */
    public JedisCacheConfiguration computePrefixWith(CacheKeyPrefix cacheKeyPrefix) {

        Assert.notNull(cacheKeyPrefix, "Function for computing prefix must not be null!");

        return new JedisCacheConfiguration(ttl, cacheNullValues, true, cacheKeyPrefix, keySerializer,
                valueSerializer);
    }

    /**
     * Disable caching {@literal null} values. <br />
     * <strong>NOTE</strong> any {@link org.springframework.cache.Cache#put(Object, Object)} operation involving
     * {@literal null} value will error. Nothing will be written to Redis, nothing will be removed. An already existing
     * key will still be there afterwards with the very same value as before.
     *
     * @return new {@link JedisCacheConfiguration}.
     */
    public JedisCacheConfiguration disableCachingNullValues() {
        return new JedisCacheConfiguration(ttl, false, usePrefix, keyPrefix, keySerializer, valueSerializer);
    }

    /**
     * Disable using cache key prefixes. <br />
     * <strong>NOTE</strong>: {@link Cache#clear()} might result in unintended removal of {@literal key}s in Redis. Make
     * sure to use a dedicated Redis instance when disabling prefixes.
     *
     * @return new {@link JedisCacheConfiguration}.
     */
    public JedisCacheConfiguration disableKeyPrefix() {

        return new JedisCacheConfiguration(ttl, cacheNullValues, false, keyPrefix, keySerializer,
                valueSerializer);
    }

    /**
     * Define the {@link ConversionService} used for cache key to {@link String} conversion.
     *
     * @param conversionService must not be {@literal null}.
     * @return new {@link JedisCacheConfiguration}.
     */
    public JedisCacheConfiguration withConversionService(ConversionService conversionService) {

        Assert.notNull(conversionService, "ConversionService must not be null!");

        return new JedisCacheConfiguration(ttl, cacheNullValues, usePrefix, keyPrefix, keySerializer,
                valueSerializer);
    }

    /**
     * Define the {@link Serializer} used for de-/serializing cache keys.
     *
     * @param keySerializer must not be {@literal null}.
     * @return new {@link JedisCacheConfiguration}.
     */
    public JedisCacheConfiguration serializeKeysWith(Serializer<String> keySerializer) {

        Assert.notNull(keySerializer, "keySerializer must not be null!");

        return new JedisCacheConfiguration(ttl, cacheNullValues, usePrefix, keyPrefix, keySerializer,
                valueSerializer);
    }

    /**
     * Define the {@link Serializer} used for de-/serializing cache values.
     *
     * @param valueSerializer must not be {@literal null}.
     * @return new {@link JedisCacheConfiguration}.
     */
    public JedisCacheConfiguration serializeValuesWith(Serializer<?> valueSerializer) {

        Assert.notNull(valueSerializer, "valueSerializer must not be null!");

        return new JedisCacheConfiguration(ttl, cacheNullValues, usePrefix, keyPrefix, keySerializer,
                (Serializer<Object>) valueSerializer);
    }

    /**
     * @return never {@literal null}.
     * @deprecated since 2.0.4. Please use {@link #getKeyPrefixFor(String)}.
     */
    @Deprecated
    public Optional<String> getKeyPrefix() {
        return usePrefix() ? Optional.of(keyPrefix.compute("")) : Optional.empty();
    }

    /**
     * Get the computed {@literal key} prefix for a given {@literal cacheName}.
     *
     * @return never {@literal null}.
     * @since 2.0.4
     */
    public String getKeyPrefixFor(String cacheName) {

        Assert.notNull(cacheName, "Cache name must not be null!");

        return keyPrefix.compute(cacheName);
    }

    /**
     * @return {@literal true} if cache keys need to be prefixed with the {@link #getKeyPrefixFor(String)} if present or
     * the default which resolves to {@link Cache#getName()}.
     */
    public boolean usePrefix() {
        return usePrefix;
    }

    /**
     * @return {@literal true} if caching {@literal null} is allowed.
     */
    public boolean getAllowCacheNullValues() {
        return cacheNullValues;
    }

    /**
     * @return never {@literal null}.
     */
    public Serializer<String> getkeySerializer() {
        return keySerializer;
    }

    /**
     * @return never {@literal null}.
     */
    public Serializer<Object> getvalueSerializer() {
        return valueSerializer;
    }

    /**
     * @return The expiration time (ttl) for cache entries. Never {@literal null}.
     */
    public Duration getTtl() {
        return ttl;
    }

    /**
     * Registers default cache key converters. The following converters get registered:
     * <ul>
     * <li>{@link String} to {@link byte byte[]} using UTF-8 encoding.</li>
     * <li>{@link SimpleKey} to {@link String}</li>
     *
     * @param registry must not be {@literal null}.
     */
    public static void registerDefaultConverters(ConverterRegistry registry) {

        Assert.notNull(registry, "ConverterRegistry must not be null!");

        registry.addConverter(String.class, byte[].class, source -> source.getBytes(StandardCharsets.UTF_8));
        registry.addConverter(SimpleKey.class, String.class, SimpleKey::toString);
    }
}
