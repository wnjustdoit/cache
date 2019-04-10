package com.caiya.cache.redis.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCachePrefix;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.cache.RedisCache;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * CacheManager backed by a Simple Spring Redis (SSR) {@link RedisCache}. Spring Cache and
 * CacheManager doesn't support configuring expiration time per method (there is no dedicated parameter in cache
 * annotation to pass expiration time). This extension of {@link org.springframework.data.redis.cache.RedisCacheManager} overcomes this limitation and allow to
 * pass expiration time as a part of cache name. To define custom expiration on method as a cache name use concatenation
 * of specific cache name, separator and expiration e.g.
 * <p>
 * <pre>
 * public class UserService {
 *
 *     // cache name: userCache, cache key: "UserService:getUser:[eg:tomcat]", expiration: 300s
 *     &#064;Cacheable(value="userCache#300", key="'UserService:getUser:' + #name") or @Cacheable(value="userCache#60 * 5", key="'UserService:getUser:' + #name")
 *     public User getUser(String name) {
 *
 *     }
 * }
 * </pre>
 *
 * @author wangnan
 * @since 1.0
 */
public class ExtendedRedisCacheManager extends RedisCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(ExtendedRedisCacheManager.class);

    private static final String SCRIPT_ENGINE_NAME = "nashorn";

    private static final ScriptEngine SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName(SCRIPT_ENGINE_NAME);

    private static final Pattern CALCULATE_PATTERN = Pattern.compile("[+\\-*/%]");

    /**
     * Default {@link #setDefaultExpiration(long)} (2 hours).
     */
    public static final int DEFAULT_MAX_LIVE_TIME_SECONDS = 60 * 60 * 2;

    private long defaultExpiration = DEFAULT_MAX_LIVE_TIME_SECONDS;

    private String defaultCacheName;

    private char separator = '#';

    public ExtendedRedisCacheManager(RedisOperations redisOperations) {
        this(redisOperations, Collections.<String>emptyList());
    }

    public ExtendedRedisCacheManager(RedisOperations redisOperations, Collection<String> cacheNames) {
        super(redisOperations, cacheNames);
    }

    @Override
    public Cache getCache(String name) {
        // check cache name is blank or not
        if (name == null || name.trim().equals("")) {
            name = defaultCacheName;
        }
        String cacheName = name;

        // find separator in cache name
        int index = name.lastIndexOf(getSeparator());
        if (index < 0) {
            // try to get cache by name, use default cacheManager
            return super.getCache(cacheName);
        }

        // split name by the separator, use extended cacheManager
        cacheName = name.substring(0, index);
        if (cacheName.trim().equals("")) {
            cacheName = defaultCacheName;
        }

        // get expiration from name
        Long expiration = getExpiration(name, index);
        if (expiration == null || expiration < 0) {
            logger.warn("Default expiration time will be used for cache: '{}' because cannot parse: '{}', original name: {}", cacheName, expiration, name);
            expiration = this.defaultExpiration;
        }

        return new RedisCache(cacheName, (isUsePrefix() ? getCachePrefix().prefix(cacheName) : null), (RedisOperations<?, ?>) getRedisOperations(), expiration);
    }


    public char getSeparator() {
        return separator;
    }

    /**
     * Char that separates cache name and expiration time, default: #.
     *
     * @param separator separator between cacheName and expiration
     */
    @SuppressWarnings("unused")
    public void setSeparator(char separator) {
        this.separator = separator;
    }

    private Long getExpiration(final String name, final int separatorIndex) {
        Long expiration = null;
        String expirationAsString = name.substring(separatorIndex + 1);
        try {
            // calculate expiration, support arithmetic expressions.
            if (CALCULATE_PATTERN.matcher(expirationAsString).find()) {
                expiration = (long) Double.parseDouble(SCRIPT_ENGINE.eval(expirationAsString).toString());
            } else {
                expiration = Long.parseLong(expirationAsString);
            }
        } catch (NumberFormatException | ScriptException ex) {
            logger.error(String.format("Can not separate expiration time from cache: '%s'", name), ex);
        }

        return expiration;
    }

    @Override
    public void setUsePrefix(boolean usePrefix) {
        super.setUsePrefix(usePrefix);
    }

    @Override
    public void setCachePrefix(RedisCachePrefix cachePrefix) {
        super.setCachePrefix(cachePrefix);
    }

    public void setDefaultCacheName(String defaultCacheName) {
        this.defaultCacheName = defaultCacheName;
    }

    @Override
    public void setDefaultExpiration(long defaultExpireTime) {
        super.setDefaultExpiration(defaultExpireTime);
        this.defaultExpiration = defaultExpireTime;
    }
}
