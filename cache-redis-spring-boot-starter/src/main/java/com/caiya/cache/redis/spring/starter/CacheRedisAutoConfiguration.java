package com.caiya.cache.redis.spring.starter;

import com.caiya.cache.CacheException;
import com.caiya.cache.redis.*;
import com.caiya.cache.redis.springx.JedisCacheConfiguration;
import com.caiya.cache.redis.springx.JedisCacheManager;
import com.caiya.cache.redis.springx.JedisCacheWriter;
import com.caiya.serialization.Serializer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
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
@ConditionalOnClass({JedisCache.class, RedisOperations.class})
@AutoConfigureBefore(RedisAutoConfiguration.class)
@EnableConfigurationProperties({CacheRedisProperties.class, RedisProperties.class})
public class CacheRedisAutoConfiguration {

    @Resource
    private CacheRedisProperties cacheRedisProperties;

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public <K, V> RedisTemplate<K, V> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<K, V> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
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

    private JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(cacheRedisProperties.getMaxTotal());
        jedisPoolConfig.setMaxIdle(cacheRedisProperties.getMaxIdle());
        jedisPoolConfig.setMinIdle(cacheRedisProperties.getMinIdle());
        jedisPoolConfig.setMaxWaitMillis(cacheRedisProperties.getMaxWaitMillis());
        return jedisPoolConfig;
    }

    /**
     * 如果使用前缀，默认前缀策略： {@link com.caiya.cache.redis.springx.CacheKeyPrefix#simple }
     */
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


}
