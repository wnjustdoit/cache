package com.caiya.cache.redis.spring.starter;

import com.caiya.cache.CacheException;
import com.caiya.cache.redis.*;
import com.caiya.serialization.Serializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.Set;

@Configuration
@ConditionalOnClass({JedisCache.class, RedisOperations.class})
@AutoConfigureBefore(RedisAutoConfiguration.class)
@EnableConfigurationProperties({CacheRedisProperties.class, RedisProperties.class})
public class CacheRedisAutoConfiguration {

    @Autowired
    private CacheRedisProperties cacheRedisProperties;

    @Autowired
    @SuppressWarnings("unused")
    private RedisProperties redisProperties;

    @Autowired
    @SuppressWarnings("unused")
    private ApplicationContext applicationContext;

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


}
