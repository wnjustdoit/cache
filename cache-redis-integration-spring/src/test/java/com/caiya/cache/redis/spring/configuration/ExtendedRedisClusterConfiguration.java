package com.caiya.cache.redis.spring.configuration;

import com.caiya.cache.redis.spring.ExtendedRedisCacheManager;
import com.caiya.cache.redis.spring.component.RedisProperties;
import com.caiya.cache.redis.spring.util.Constant;
import com.caiya.serialization.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Redis Cluster相关配置.
 *
 * @author wangnan
 * @since 1.0
 */
@Configuration
@EnableCaching
public class ExtendedRedisClusterConfiguration extends CachingConfigurerSupport {

    private static final Logger logger = LoggerFactory.getLogger(ExtendedRedisClusterConfiguration.class);

    @Resource
    private RedisProperties redisProperties;

    private JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(redisProperties.getMaxTotal());
        jedisPoolConfig.setMaxIdle(redisProperties.getMaxIdle());
        jedisPoolConfig.setMinIdle(redisProperties.getMinIdle());
        jedisPoolConfig.setMaxWaitMillis(redisProperties.getMaxWaitMillis());
        return jedisPoolConfig;
    }

    private RedisClusterConfiguration redisClusterConfiguration() {
        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
        String[] hostNames = redisProperties.getHostNames().split(",");
        Set<RedisNode> redisNodes = new HashSet<>();
        for (String hostName : hostNames) {
            String host = hostName.split(":")[0];
            int port = Integer.parseInt(hostName.split(":")[1]);
            redisNodes.add(new RedisClusterNode(host, port));
        }
        redisClusterConfiguration.setClusterNodes(redisNodes);
        redisClusterConfiguration.setMaxRedirects(redisProperties.getMaxRedirects());
        if (redisProperties.getPassword() != null) {
            redisClusterConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
        return redisClusterConfiguration;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory(redisClusterConfiguration(), jedisPoolConfig());
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        JdkSerializationRedisSerializer jdkSerializationRedisSerializer = new JdkSerializationRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(jdkSerializationRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(jdkSerializationRedisSerializer);
        return redisTemplate;
    }

    @Override
    @Bean
    public CacheManager cacheManager() {
        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(jedisConnectionFactory());
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(Constant.DEFAULT_EXPIRATION))
                .prefixKeysWith(Constant.DEFAULT_CACHE_NAME + ":");
        ExtendedRedisCacheManager cacheManager = new ExtendedRedisCacheManager(cacheWriter, cacheConfiguration);
        cacheManager.setDefaultCacheName(Constant.DEFAULT_CACHE_NAME);
        return cacheManager;
    }


    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                logger.error("handleCacheGetError, cacheName:{}, key:{}, exception:", cache.getName(), key, exception);
                // 这里暂时先抛异常
                throw exception;
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                logger.error("handleCachePutError, cacheName:{}, key:{}, value:{}, exception:", cache.getName(), key, value, exception);
                throw exception;
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                logger.error("handleCacheEvictError, cacheName:{}, key:{}, exception:", cache.getName(), key, exception);
                throw exception;
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                logger.error("handleCacheClearError, cacheName:{}, exception:", cache.getName(), exception);
                throw exception;
            }
        };
    }
}
