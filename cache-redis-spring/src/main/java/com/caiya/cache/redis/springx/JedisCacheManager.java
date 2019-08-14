package com.caiya.cache.redis.springx;

import com.caiya.cache.redis.JedisConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * CacheManager backed by a Simple Spring Redis (SSR) {@link JedisSpringCache}. Spring Cache and
 * CacheManager doesn't support configuring expiration time per method (there is no dedicated parameter in cache
 * annotation to pass expiration time). This extension of {@link AbstractCacheManager} overcomes this limitation and allow to
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
 * @since 1.1.1
 */
public class JedisCacheManager extends AbstractCacheManager {

    private final static Logger logger = LoggerFactory.getLogger(JedisCacheManager.class);

    private final JedisCacheWriter cacheWriter;
    private final JedisCacheConfiguration defaultCacheConfig;
    private final Map<String, JedisCacheConfiguration> initialCacheConfiguration;
    private final boolean allowInFlightCacheCreation;

    private static final String SCRIPT_ENGINE_NAME = "nashorn";

    private static final ScriptEngine SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName(SCRIPT_ENGINE_NAME);

    private static final Pattern CALCULATE_PATTERN = Pattern.compile("[+\\-*/%]");

    private String defaultCacheName;

    private char separator = '#';


    /**
     * Creates new {@link JedisCacheManager} using given {@link JedisCacheWriter} and default
     * {@link JedisCacheConfiguration}.
     *
     * @param cacheWriter                must not be {@literal null}.
     * @param defaultCacheConfiguration  must not be {@literal null}. Maybe just use
     *                                   {@link JedisCacheConfiguration#defaultCacheConfig()}.
     * @param allowInFlightCacheCreation allow create unconfigured caches.
     * @since 2.0.4
     */
    private JedisCacheManager(JedisCacheWriter cacheWriter, JedisCacheConfiguration defaultCacheConfiguration,
                              boolean allowInFlightCacheCreation) {

        Assert.notNull(cacheWriter, "CacheWriter must not be null!");
        Assert.notNull(defaultCacheConfiguration, "DefaultCacheConfiguration must not be null!");

        this.cacheWriter = cacheWriter;
        this.defaultCacheConfig = defaultCacheConfiguration;
        this.initialCacheConfiguration = new LinkedHashMap<>();
        this.allowInFlightCacheCreation = allowInFlightCacheCreation;
    }

    /**
     * Creates new {@link JedisCacheManager} using given {@link JedisCacheWriter} and default
     * {@link JedisCacheConfiguration}.
     *
     * @param cacheWriter               must not be {@literal null}.
     * @param defaultCacheConfiguration must not be {@literal null}. Maybe just use
     *                                  {@link JedisCacheConfiguration#defaultCacheConfig()}.
     */
    public JedisCacheManager(JedisCacheWriter cacheWriter, JedisCacheConfiguration defaultCacheConfiguration) {
        this(cacheWriter, defaultCacheConfiguration, true);
    }

    /**
     * Creates new {@link JedisCacheManager} using given {@link JedisCacheWriter} and default
     * {@link JedisCacheConfiguration}.
     *
     * @param cacheWriter               must not be {@literal null}.
     * @param defaultCacheConfiguration must not be {@literal null}. Maybe just use
     *                                  {@link JedisCacheConfiguration#defaultCacheConfig()}.
     * @param initialCacheNames         optional set of known cache names that will be created with given
     *                                  {@literal defaultCacheConfiguration}.
     */
    public JedisCacheManager(JedisCacheWriter cacheWriter, JedisCacheConfiguration defaultCacheConfiguration,
                             String... initialCacheNames) {

        this(cacheWriter, defaultCacheConfiguration, true, initialCacheNames);
    }

    /**
     * Creates new {@link JedisCacheManager} using given {@link JedisCacheWriter} and default
     * {@link JedisCacheConfiguration}.
     *
     * @param cacheWriter                must not be {@literal null}.
     * @param defaultCacheConfiguration  must not be {@literal null}. Maybe just use
     *                                   {@link JedisCacheConfiguration#defaultCacheConfig()}.
     * @param allowInFlightCacheCreation if set to {@literal true} no new caches can be acquire at runtime but limited to
     *                                   the given list of initial cache names.
     * @param initialCacheNames          optional set of known cache names that will be created with given
     *                                   {@literal defaultCacheConfiguration}.
     * @since 2.0.4
     */
    public JedisCacheManager(JedisCacheWriter cacheWriter, JedisCacheConfiguration defaultCacheConfiguration,
                             boolean allowInFlightCacheCreation, String... initialCacheNames) {

        this(cacheWriter, defaultCacheConfiguration, allowInFlightCacheCreation);

        for (String cacheName : initialCacheNames) {
            this.initialCacheConfiguration.put(cacheName, defaultCacheConfiguration);
        }
    }

    /**
     * Creates new {@link JedisCacheManager} using given {@link JedisCacheWriter} and default
     * {@link JedisCacheConfiguration}.
     *
     * @param cacheWriter                must not be {@literal null}.
     * @param defaultCacheConfiguration  must not be {@literal null}. Maybe just use
     *                                   {@link JedisCacheConfiguration#defaultCacheConfig()}.
     * @param initialCacheConfigurations Map of known cache names along with the configuration to use for those caches.
     *                                   Must not be {@literal null}.
     */
    public JedisCacheManager(JedisCacheWriter cacheWriter, JedisCacheConfiguration defaultCacheConfiguration,
                             Map<String, JedisCacheConfiguration> initialCacheConfigurations) {

        this(cacheWriter, defaultCacheConfiguration, initialCacheConfigurations, true);
    }

    /**
     * Creates new {@link JedisCacheManager} using given {@link JedisCacheWriter} and default
     * {@link JedisCacheConfiguration}.
     *
     * @param cacheWriter                must not be {@literal null}.
     * @param defaultCacheConfiguration  must not be {@literal null}. Maybe just use
     *                                   {@link JedisCacheConfiguration#defaultCacheConfig()}.
     * @param initialCacheConfigurations Map of known cache names along with the configuration to use for those caches.
     *                                   Must not be {@literal null}.
     * @param allowInFlightCacheCreation if set to {@literal false} this cache manager is limited to the initial cache
     *                                   configurations and will not create new caches at runtime.
     * @since 2.0.4
     */
    public JedisCacheManager(JedisCacheWriter cacheWriter, JedisCacheConfiguration defaultCacheConfiguration,
                             Map<String, JedisCacheConfiguration> initialCacheConfigurations, boolean allowInFlightCacheCreation) {

        this(cacheWriter, defaultCacheConfiguration, allowInFlightCacheCreation);

        Assert.notNull(initialCacheConfigurations, "InitialCacheConfigurations must not be null!");

        this.initialCacheConfiguration.putAll(initialCacheConfigurations);
    }

    /**
     * Create a new {@link JedisCacheManager} with defaults applied.
     * <dl>
     * <dt>locking</dt>
     * <dd>disabled</dd>
     * <dt>cache configuration</dt>
     * <dd>{@link JedisCacheConfiguration#defaultCacheConfig()}</dd>
     * <dt>initial caches</dt>
     * <dd>none</dd>
     * <dt>transaction aware</dt>
     * <dd>no</dd>
     * <dt>in-flight cache creation</dt>
     * <dd>enabled</dd>
     * </dl>
     *
     * @param connectionFactory must not be {@literal null}.
     * @return new instance of {@link JedisCacheManager}.
     */
    public static JedisCacheManager create(JedisConnectionFactory connectionFactory) {

        Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");

        return new JedisCacheManager(new DefaultJedisCacheWriter(connectionFactory),
                JedisCacheConfiguration.defaultCacheConfig());
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
        JedisCacheConfiguration currentCacheConfiguration = defaultCacheConfig;
        if (expiration != null && expiration >= 0) {
            currentCacheConfiguration = defaultCacheConfig.entryTtl(Duration.ofSeconds(expiration));
        } else {
            logger.warn("Default expiration time will be used for cache: '{}' because cannot parse: '{}', original name: {}", cacheName, expiration, name);
        }

        return new JedisSpringCache(cacheName, cacheWriter, currentCacheConfiguration);
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        List<JedisSpringCache> caches = new LinkedList<>();

        for (Map.Entry<String, JedisCacheConfiguration> entry : initialCacheConfiguration.entrySet()) {
            caches.add(createRedisCache(entry.getKey(), entry.getValue()));
        }

        return caches;
    }

    @Override
    protected Cache getMissingCache(String name) {
        return allowInFlightCacheCreation ? createRedisCache(name, defaultCacheConfig) : null;
    }

    /**
     * Configuration hook for creating {@link JedisSpringCache} with given name and {@code cacheConfig}.
     *
     * @param name        must not be {@literal null}.
     * @param cacheConfig can be {@literal null}.
     * @return never {@literal null}.
     */
    protected JedisSpringCache createRedisCache(String name, JedisCacheConfiguration cacheConfig) {
        return new JedisSpringCache(name, cacheWriter, cacheConfig != null ? cacheConfig : defaultCacheConfig);
    }

    public char getSeparator() {
        return separator;
    }

    public JedisCacheManager setSeparator(char separator) {
        this.separator = separator;
        return this;
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
        } catch (NumberFormatException | ScriptException e) {
            logger.error(String.format("Can not separate expiration time from cache: '%s'", name), e);
        }

        return expiration;
    }

    public JedisCacheManager setDefaultCacheName(String defaultCacheName) {
        this.defaultCacheName = defaultCacheName;
        return this;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        if (StringUtils.isEmpty(this.defaultCacheName)) {
            throw new IllegalArgumentException("defaultCacheName cannot be empty.");
        }
    }
}
