package com.caiya.cache.redis.spring;

import com.caiya.cache.CacheApi;
import com.caiya.cache.RedisConstant;
import com.caiya.cache.ScanResult;
import com.caiya.cache.SetOption;
import com.caiya.cache.redis.RedisCacheKey;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationUtils;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.util.JedisClusterCRC16;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis Cache Implementation, Base on spring-data-redis.
 *
 * @author wangnan
 * @see org.springframework.data.redis.core.RedisOperations
 * @see org.springframework.data.redis.core.RedisTemplate
 * <p>
 * @since 1.0
 */
public class RedisCache<K, V> implements CacheApi<K, V> {

    /**
     * 字符串序列化方式
     */
    private static final StringRedisSerializer STRING_REDIS_SERIALIZER = new StringRedisSerializer();

    /**
     * JDK序列化方式
     */
    private static final JdkSerializationRedisSerializer JDK_SERIALIZATION_REDIS_SERIALIZER = new JdkSerializationRedisSerializer();

    /**
     * 默认序列化方式
     */
    private static final RedisSerializer DEFAULT_REDIS_SERIALIZER = JDK_SERIALIZATION_REDIS_SERIALIZER;

    /**
     * Cache Name
     */
    private String name;

    /**
     * Cache key prefix
     */
    private byte[] keyPrefix;

    private RedisOperations<K, V> redisOperations;

    private RedisSerializer keySerializer = DEFAULT_REDIS_SERIALIZER;
    private RedisSerializer valueSerializer = DEFAULT_REDIS_SERIALIZER;
    private RedisSerializer hashKeySerializer = DEFAULT_REDIS_SERIALIZER;
    private RedisSerializer hashValueSerializer = DEFAULT_REDIS_SERIALIZER;

    public RedisCache(RedisOperations<K, V> redisOperations) {
        this(null, null, redisOperations);
    }

    public RedisCache(byte[] keyPrefix, RedisOperations<K, V> redisOperations) {
        this(null, keyPrefix, redisOperations);
    }

    public RedisCache(String name, byte[] keyPrefix, RedisOperations<K, V> redisOperations) {
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.redisOperations = redisOperations;
        if (redisOperations.getKeySerializer() != null) {
            this.keySerializer = redisOperations.getKeySerializer();
        }
        if (redisOperations.getValueSerializer() != null) {
            this.valueSerializer = redisOperations.getValueSerializer();
        }
        if (redisOperations.getHashKeySerializer() != null) {
            this.hashKeySerializer = redisOperations.getHashKeySerializer();
        }
        if (redisOperations.getHashValueSerializer() != null) {
            this.hashValueSerializer = redisOperations.getHashValueSerializer();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public long del(final K... keys) {
        if (keys.length == 1) {
            return redisOperations.execute((RedisCallback<Long>) connection -> connection.del(rawKey(keys[0])));
        }
        // group by hash slot
        Map<Integer, List<byte[]>> slotKeyMap = new HashMap<>();
        for (K key : keys) {
            byte[] rawKey = rawKey(key);
            int slot = JedisClusterCRC16.getSlot(rawKey);
            if (!slotKeyMap.containsKey(slot)) {
                slotKeyMap.put(slot, new ArrayList<>());
            }
            slotKeyMap.get(slot)
                    .add(rawKey);
        }
        return redisOperations.execute((RedisCallback<Long>) connection -> {
            long result = 0;
            for (List<byte[]> rawKeys : slotKeyMap.values()) {
                result += connection.del(rawKeys.toArray(new byte[rawKeys.size()][]));
            }
            return result;
        });
    }

    private void set(final byte[] key, final byte[] value, final long liveTime) {
        redisOperations.execute((RedisCallback<Void>) connection -> {
            connection.setEx(key, liveTime, value);
            return null;
        });
    }

    @Override
    public void set(K key, V value, long liveTime) {
        this.set(rawKey(key), rawValue(value), liveTime);
    }

    @Override
    public void set(K key, V value, long liveTime, TimeUnit timeUnit) {
        if (liveTime <= 0) {// never expires, regard redis as a DB
            redisOperations.execute((RedisCallback<Void>) connection -> {
                connection.set(rawKey(key), rawValue(value));
                return null;
            });
            return;
        }

        if (timeUnit == null) {
            throw new IllegalArgumentException("the TimeUnit of liveTime cannot be null");
        }

        if (Objects.equals(TimeUnit.SECONDS, timeUnit)) {
            set(key, value, liveTime);
        } else if (Objects.equals(TimeUnit.MILLISECONDS, timeUnit)) {
            redisOperations.execute((RedisCallback<Void>) connection -> {
                connection.pSetEx(rawKey(key), liveTime, rawValue(value));
                return null;
            });
        } else {
            throw new IllegalArgumentException("Unsupported TimeUnit of the live time");
        }
    }

    @Override
    public String set(K key, V value, SetOption setOption, long expirationTime, TimeUnit timeUnit) {
        if (setOption == null)
            throw new IllegalArgumentException("set option cannot be null");
        if (expirationTime <= 0)
            throw new IllegalArgumentException("expiration time cannot be zero or negative");

        return redisOperations.execute((RedisCallback<String>) connection -> {
            RedisStringCommands.SetOption redisSetOption;
            if (Objects.equals(SetOption.SET_IF_ABSENT, setOption)) {
                redisSetOption = RedisStringCommands.SetOption.ifAbsent();
            } else if (Objects.equals(SetOption.SET_IF_PRESENT, setOption)) {
                redisSetOption = RedisStringCommands.SetOption.ifPresent();
            } else {
                throw new IllegalArgumentException("Unsupported set options");
            }
            Boolean result = connection.set(rawKey(key), rawValue(value), Expiration.from(expirationTime, timeUnit), redisSetOption);
            return Boolean.TRUE.equals(result) ? "OK" : null;
        });
    }

    @Override
    public V get(final K key) {
        return redisOperations.execute((RedisCallback<V>) connection -> {
            byte[] bs = connection.get(rawKey(key));
            return deserializeValue(bs);
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final K key, RedisSerializer keySerializer, RedisSerializer valueSerializer) {
        return redisOperations.execute((RedisCallback<T>) connection -> {
            byte[] bs = connection.get(rawKey(key, keySerializer));
            return (T) deserializeValue(bs, valueSerializer);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    @Deprecated
    public Set<K> keys(final String pattern) {
        return redisOperations.execute((RedisCallback<Set<K>>) connection -> {
            Set<byte[]> rawKeys = connection.keys(rawKey(pattern));
            return SerializationUtils.deserialize(rawKeys, keySerializer);
        });
    }

    @Override
    @Deprecated
    public List<K> scan(final String pattern, final long count) {
        throw new UnsupportedOperationException("Unsupported Command Operation");
    }

    @Override
    @Deprecated
    public ScanResult<String> scan(String cursor, String pattern, long count) {
        throw new UnsupportedOperationException("Unsupported Command Operation");
    }

    @Override
    public boolean exists(final K key) {
        return redisOperations.execute((RedisCallback<Boolean>) connection -> connection.exists(rawKey(key)));
    }

    @Override
    public boolean expire(final K key, final long liveTime) {
        return redisOperations.execute((RedisCallback<Boolean>) connection -> connection.expire(rawKey(key), liveTime));
    }

    @Override
    public long ttl(final K key) {
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.ttl(rawKey(key)));
    }

    @Override
    public Long incr(K key) {
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.incr(rawKey(key)));
    }

    @Override
    public Long incrBy(K key, long integer) {
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.incrBy(rawKey(key), integer));
    }

    @Override
    public Long decr(K key) {
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.decr(rawKey(key)));
    }

    @Override
    public Long decrBy(K key, long integer) {
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.decrBy(rawKey(key), integer));
    }

    /**
     * @param oldKey must not be {@literal null}.
     * @param newKey must not be {@literal null}.
     * @return NULL
     * @see #rename(K, K, com.caiya.cache.RedisConstant.Operation)
     */
    @Override
    public String rename(K oldKey, K newKey) {
        redisOperations.execute((RedisCallback<Void>) connection -> {
            connection.rename(rawKey(oldKey), rawKey(newKey));
            return null;
        });
        return null;
    }

    /**
     * Not perfected. When two keys are not the same slot, expire time is not copied.
     * <p>
     * What else, hash operation is not supported in this situation.
     *
     * @param oldKey must not be {@literal null}.
     * @param newKey must not be {@literal null}.
     * @return NULL
     * @see org.springframework.data.redis.connection.jedis.JedisClusterConnection#rename(byte[], byte[])
     */
    @Override
    public String rename(K oldKey, K newKey, RedisConstant.Operation operation) {
        return rename(oldKey, newKey);
    }

    @Override
    public Long append(K key, V value) {
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.append(rawKey(key), rawValue(value)));
    }

    @Override
    public void flushDB() {
        redisOperations.execute((RedisCallback<Void>) connection -> {
            connection.flushDb();
            return null;
        });
    }

    @Override
    public long dbSize() {
        return redisOperations.execute(RedisServerCommands::dbSize);
    }

    @Override
    public String ping() {
        return redisOperations.execute(RedisConnectionCommands::ping);
    }

    @Override
    public <HK, HV> boolean hSet(final K key, final HK field, final HV value) {
        return redisOperations.execute((RedisCallback<Boolean>) connection -> connection.hSet(rawKey(key), rawHashKey(field), rawHashValue(value)));
    }

    @Override
    public <HK, HV> HV hGet(final K key, final HK field) {
        return redisOperations.execute((RedisCallback<HV>) connection -> deserializeHashValue(connection.hGet(rawKey(key), rawHashKey(field))));
    }

    @Override
    public <HK> Boolean hExists(K key, HK field) {
        return redisOperations.execute((RedisCallback<Boolean>) connection -> connection.hExists(rawKey(key), rawHashKey(field)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <HK> Set<HK> hKeys(final K key) {
        return redisOperations.execute((RedisCallback<Set<HK>>) connection -> SerializationUtils.deserialize(connection.hKeys(rawKey(key)), hashKeySerializer));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <HK> long hDel(final K key, final HK... fields) {
        return redisOperations.execute((RedisCallback<Long>) connection -> {
            byte[][] fieldBytes = new byte[fields.length][];
            int index = 0;
            for (HK field : fields) {
                fieldBytes[index++] = rawHashKey(field);
            }
            return connection.hDel(rawKey(key), fieldBytes);
        });
    }

    @Override
    public void hMSet(final K key, final Map<String, Object> hashes) {
        redisOperations.execute((RedisCallback<Void>) connection -> {
            Map<byte[], byte[]> newHashes = new HashMap<>(hashes.size());
            for (Map.Entry<String, Object> entry : hashes.entrySet()) {
                newHashes.put(rawHashKey(entry.getKey()), rawHashValue(entry.getValue()));
            }
            connection.hMSet(rawKey(key), newHashes);
            return null;
        });
    }

    @Override
    public <HK, HV> Map<HK, HV> hGetAll(K key) {
        return redisOperations.execute((RedisCallback<Map<HK, HV>>) connection -> {
            Map<HK, HV> result = new HashMap<>();
            Map<byte[], byte[]> hashes = connection.hGetAll(rawKey(key));
            for (Map.Entry<byte[], byte[]> entry : hashes.entrySet()) {
                result.put(deserializeHashKey(entry.getKey()), deserializeHashValue(entry.getValue()));
            }
            return result;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public long lPush(final K key, final V... values) {
        return redisOperations.execute((RedisCallback<Long>) connection -> {
            byte[][] bytes = new byte[values.length][];
            int index = 0;
            for (Object value : values) {
                bytes[index++] = rawValue(value);
            }
            return connection.lPush(rawKey(key), bytes);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<V> lRange(final K key, final long begin, final long end) {
        return redisOperations.execute((RedisCallback<List<V>>) connection -> {
            List<byte[]> rawValues = connection.lRange(rawKey(key), begin, end);
            return SerializationUtils.deserialize(rawValues, valueSerializer);
        });
    }

    @Override
    public long lRem(final K key, final long count, final V value) {
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.lRem(rawKey(key), count, rawValue(value)));
    }

    @Override
    public void lTrim(final K key, final long begin, final long end) {
        redisOperations.execute((RedisCallback<Void>) connection -> {
            connection.lTrim(rawKey(key), begin, end);
            return null;
        });
    }

    @Override
    public V lPop(final K key) {
        return redisOperations.execute((RedisCallback<V>) connection -> deserializeValue(connection.lPop(rawKey(key))));
    }

    @Override
    public long lLen(final K key) {
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.lLen(rawKey(key)));
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        byte[][] paramsBytes = new byte[params.length][];
        int index = 0;
        for (String param : params) {
            paramsBytes[index++] = STRING_REDIS_SERIALIZER.serialize(param);
        }
        return redisOperations.execute((RedisCallback<Long>) connection -> connection.eval(STRING_REDIS_SERIALIZER.serialize(script), ReturnType.VALUE, keyCount, paramsBytes));
    }

    @SuppressWarnings("unchecked")
    private byte[] rawKey(Object key) {
        if (keyPrefix != null) {
            return new RedisCacheKey(key).usePrefix(keyPrefix).getKeyBytes();
        }

        return keySerializer.serialize(key);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawKey(Object key, RedisSerializer keySerializer) {
        if (keyPrefix != null) {
            return new RedisCacheKey(key).usePrefix(keyPrefix).getKeyBytes();
        }

        return keySerializer.serialize(key);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawValue(Object value) {
        return valueSerializer.serialize(value);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawHashKey(Object key) {
        return hashKeySerializer.serialize(key);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawHashValue(Object value) {
        return hashValueSerializer.serialize(value);
    }

    @SuppressWarnings("unchecked")
    private V deserializeValue(byte[] value) {
        return (V) valueSerializer.deserialize(value);
    }

    @SuppressWarnings("unchecked")
    private V deserializeValue(byte[] value, RedisSerializer valueSerializer) {
        return (V) valueSerializer.deserialize(value);
    }

    @SuppressWarnings("unchecked")
    private <HK> HK deserializeHashKey(byte[] hk) {
        return (HK) hashKeySerializer.deserialize(hk);
    }

    @SuppressWarnings("unchecked")
    private <HV> HV deserializeHashValue(byte[] value) {
        return (HV) hashValueSerializer.deserialize(value);
    }

    @SuppressWarnings("unchecked")
    public RedisSerializer getKeySerializer() {
        return keySerializer;
    }

    @SuppressWarnings("unchecked")
    public RedisSerializer getValueSerializer() {
        return valueSerializer;
    }

    @SuppressWarnings("unchecked")
    public RedisSerializer getHashKeySerializer() {
        return hashKeySerializer;
    }

    @SuppressWarnings("unchecked")
    public RedisSerializer getHashValueSerializer() {
        return hashValueSerializer;
    }

    public void setKeyPrefix(byte[] keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public byte[] getKeyPrefix() {
        return keyPrefix;
    }
}