package com.caiya.cache.redis.springx.configuration;

import com.caiya.cache.CacheException;
import com.caiya.cache.redis.JedisConnectionFactory;
import com.caiya.cache.redis.RedisConnectionFactory;
import com.caiya.cache.redis.RedisTemplate;
import com.caiya.cache.redis.springx.JedisCacheConfiguration;
import com.caiya.cache.redis.springx.JedisCacheManager;
import com.caiya.cache.redis.springx.JedisCacheWriter;
import com.caiya.cache.redis.springx.component.CacheRedisProperties;
import com.caiya.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableCaching
public class CacheRedisConfiguration extends CachingConfigurerSupport {

    private static final Logger logger = LoggerFactory.getLogger(CacheRedisConfiguration.class);

    @Resource
    private CacheRedisProperties cacheRedisProperties;

    private JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(cacheRedisProperties.getMaxTotal());
        jedisPoolConfig.setMaxIdle(cacheRedisProperties.getMaxIdle());
        jedisPoolConfig.setMinIdle(cacheRedisProperties.getMinIdle());
        jedisPoolConfig.setMaxWaitMillis(cacheRedisProperties.getMaxWaitMillis());
        return jedisPoolConfig;
    }


    @Bean(destroyMethod = "destroy")
    public RedisConnectionFactory jedisConnectionFactory() {
        String[] hostNameArray = cacheRedisProperties.getHostNames().split(",");
        Set<HostAndPort> clusterNodes = new HashSet<>();
        for (String hostName : hostNameArray) {
            String host = hostName.split(":")[0];
            int port = Integer.parseInt(hostName.split(":")[1]);
            clusterNodes.add(new HostAndPort(host, port));
        }
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setClusterNodes(clusterNodes);
        jedisConnectionFactory.setPassword(cacheRedisProperties.getPassword());
        jedisConnectionFactory.setPoolConfig(jedisPoolConfig());
        jedisConnectionFactory.setMaxRedirects(cacheRedisProperties.getMaxRedirects());
        jedisConnectionFactory.setCacheName(cacheRedisProperties.getDefaultCacheName());
        if (cacheRedisProperties.isUseKeyPrefix()) {
            if (cacheRedisProperties.getKeyPrefix() != null) {
                jedisConnectionFactory.setKeyPrefix(cacheRedisProperties.getKeyPrefix().getBytes(Charset.defaultCharset()));
            } else {
                // 如果使用前缀，默认前缀策略
                jedisConnectionFactory.setKeyPrefix((cacheRedisProperties.getDefaultCacheName() + ":").getBytes(Charset.defaultCharset()));
            }
        }
        if (cacheRedisProperties.getKeySerializer() != null) {
            jedisConnectionFactory.setKeySerializer(getSerializerInstance(cacheRedisProperties.getKeySerializer()));
        }
        if (cacheRedisProperties.getValueSerializer() != null) {
            jedisConnectionFactory.setValueSerializer(getSerializerInstance(cacheRedisProperties.getValueSerializer()));
        }
        if (cacheRedisProperties.getHashKeySerializer() != null) {
            jedisConnectionFactory.setHashKeySerializer(getSerializerInstance(cacheRedisProperties.getHashKeySerializer()));
        }
        if (cacheRedisProperties.getHashValueSerializer() != null) {
            jedisConnectionFactory.setHashValueSerializer(getSerializerInstance(cacheRedisProperties.getHashValueSerializer()));
        }
        return jedisConnectionFactory;
    }

    private static Serializer<?> getSerializerInstance(String serializerString) {
        try {
            return (Serializer<?>) Class.forName(serializerString).newInstance();
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            throw new CacheException("get serializer object failed");
        }
    }

    @Bean
    public <K, V> RedisTemplate<K, V> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<K, V> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory jedisConnectionFactory) {
        JedisCacheWriter cacheWriter = JedisCacheWriter.nonLockingRedisCacheWriter(jedisConnectionFactory);
        JedisCacheConfiguration cacheConfiguration = JedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(cacheRedisProperties.getDefaultExpirationSeconds()));
        if (cacheRedisProperties.isUseKeyPrefix()) {
            if (cacheRedisProperties.getKeyPrefix() != null) {
                cacheConfiguration.prefixKeysWith(cacheRedisProperties.getKeyPrefix());
            }
        } else {
            cacheConfiguration.disableKeyPrefix();
        }
        return new JedisCacheManager(cacheWriter, cacheConfiguration)
                .setDefaultCacheName(cacheRedisProperties.getDefaultCacheName());
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logger.error("handleCacheGetError, cacheName:{}, key:{}, exception:", cache.getName(), key, exception);
                throw new CacheException(exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logger.error("handleCachePutError, cacheName:{}, key:{}, value:{}, exception:", cache.getName(), key, value, exception);
                throw new CacheException(exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logger.error("handleCacheEvictError, cacheName:{}, key:{}, exception:", cache.getName(), key, exception);
                throw new CacheException(exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logger.error("handleCacheClearError, cacheName:{}, exception:", cache.getName(), exception);
                throw new CacheException(exception);
            }
        };
    }


}
