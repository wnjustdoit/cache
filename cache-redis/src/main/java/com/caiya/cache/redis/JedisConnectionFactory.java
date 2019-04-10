package com.caiya.cache.redis;

import com.caiya.serialization.Serializer;
import com.caiya.serialization.jdk.JdkSerializationSerializer;
import com.caiya.serialization.jdk.StringSerializer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.util.Set;

/**
 * Connection factory creating <a href="http://github.com/xetorthio/jedis">Jedis</a> based connections.
 *
 * @author wangnan
 * @since 1.0
 */
public class JedisConnectionFactory implements RedisConnectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(JedisConnectionFactory.class);

    private volatile boolean initialized;

    private JedisPoolConfig poolConfig = new JedisPoolConfig();

    private int timeout = Protocol.DEFAULT_TIMEOUT;
    private String password;
    private Set<HostAndPort> clusterNodes;
    private Integer maxRedirects;
    private JedisCache<?, ?> jedisCache;

    private String cacheName;
    private byte[] keyPrefix;

    private static final Serializer<?> STRING_SERIALIZER = new StringSerializer();

    private static final Serializer<?> JDK_SERIALIZATION_SERIALIZER = new JdkSerializationSerializer();

    private Serializer<?> defaultSerializer = JDK_SERIALIZATION_SERIALIZER;

    private Serializer<?> keySerializer = null;
    private Serializer<?> valueSerializer = null;
    private Serializer<?> hashKeySerializer = null;
    private Serializer<?> hashValueSerializer = null;

    public JedisConnectionFactory() {
    }

    public JedisConnectionFactory(Set<HostAndPort> clusterNodes) {
        this.clusterNodes = clusterNodes;
    }

    @Override
    public void afterPropertiesSet() {
        synchronized (this) {
            if (!initialized) {
                if (keySerializer == null) {
                    keySerializer = defaultSerializer;
                }
                if (valueSerializer == null) {
                    valueSerializer = defaultSerializer;
                }
                if (hashKeySerializer == null) {
                    hashKeySerializer = defaultSerializer;
                }
                if (hashValueSerializer == null) {
                    hashValueSerializer = defaultSerializer;
                }

                jedisCache = createCluster();
                jedisCache.setName(getCacheName());
                jedisCache.setKeyPrefix(getKeyPrefix());
                jedisCache.setKeySerializer(keySerializer);
                jedisCache.setValueSerializer(valueSerializer);
                jedisCache.setHashKeySerializer(hashKeySerializer);
                jedisCache.setHashValueSerializer(hashValueSerializer);

                initialized = true;
            }
        }
    }

    @Override
    public void destroy() {
        if (jedisCache != null) {
            try {
                jedisCache.close();
            } catch (Exception ex) {
                logger.warn("Cannot properly close Jedis cluster", ex);
            }
        }
    }

    @Override
    public RedisConnection getConnection() {
        // traditionally generate new Jedis instance here

        return getClusterConnection();
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        if (!initialized) {
            afterPropertiesSet();
        }
        if (jedisCache == null) {
            throw new IllegalArgumentException("Cluster is not configured!");
        }

        return new JedisClusterConnection(jedisCache);
    }

    private JedisCache<?, ?> createCluster() {
        return createCluster(this.clusterNodes, this.poolConfig);
    }

    /**
     * Creates {@link JedisCache} for given {@link GenericObjectPoolConfig}.
     *
     * @param clusterNodes must not be {@literal null}.
     * @param poolConfig   can be {@literal null}.
     * @return JedisCache
     * @since 1.7
     */
    protected JedisCache<?, ?> createCluster(Set<HostAndPort> clusterNodes, GenericObjectPoolConfig poolConfig) {
        if (clusterNodes == null || clusterNodes.isEmpty())
            throw new IllegalArgumentException("Cluster configuration must not be null!");

        int redirects = getMaxRedirects() != null ? getMaxRedirects() : 5;

        JedisCluster jedisCluster = (getPassword() == null)
                ? new JedisCluster(clusterNodes, timeout, timeout, redirects, password, poolConfig)
                : new JedisCluster(clusterNodes, timeout, redirects, poolConfig);
        return new JedisCache<>(jedisCluster);
    }

    public void setPoolConfig(JedisPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }

    public JedisPoolConfig getPoolConfig() {
        return poolConfig;
    }

    public void setClusterNodes(Set<HostAndPort> clusterNodes) {
        this.clusterNodes = clusterNodes;
    }

    public Set<HostAndPort> getClusterNodes() {
        return clusterNodes;
    }

    public Integer getMaxRedirects() {
        return maxRedirects != null && maxRedirects > Integer.MIN_VALUE ? maxRedirects : null;
    }

    public void setMaxRedirects(int maxRedirects) {
        if (maxRedirects < 0)
            throw new IllegalArgumentException("MaxRedirects must be greater or equal to 0");

        this.maxRedirects = maxRedirects;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setKeyPrefix(byte[] keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public byte[] getKeyPrefix() {
        return keyPrefix;
    }


    public Serializer<?> getKeySerializer() {
        return keySerializer;
    }

    public Serializer<?> getValueSerializer() {
        return valueSerializer;
    }

    public Serializer<?> getHashKeySerializer() {
        return hashKeySerializer;
    }

    public Serializer<?> getHashValueSerializer() {
        return hashValueSerializer;
    }

    public void setKeySerializer(Serializer<?> keySerializer) {
        this.keySerializer = keySerializer;
    }

    public void setValueSerializer(Serializer<?> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public void setHashKeySerializer(Serializer<?> hashKeySerializer) {
        this.hashKeySerializer = hashKeySerializer;
    }

    public void setHashValueSerializer(Serializer<?> hashValueSerializer) {
        this.hashValueSerializer = hashValueSerializer;
    }

    public void setDefaultSerializer(Serializer<?> defaultSerializer) {
        this.defaultSerializer = defaultSerializer;
    }

}
