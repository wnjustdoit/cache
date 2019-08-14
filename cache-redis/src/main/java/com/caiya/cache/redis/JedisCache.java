package com.caiya.cache.redis;

import com.caiya.cache.*;
import com.caiya.serialization.Serializer;
import com.caiya.serialization.jdk.JdkSerializationSerializer;
import com.caiya.serialization.jdk.StringSerializer;
import com.caiya.serialization.util.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.ScanParams;
import redis.clients.util.JedisClusterCRC16;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis Cache Implementation, Based on JedisCluster.
 * Method {@link #close()} needs to be invoked after using the {@link JedisCache} client,
 * this operation likes {@link JedisCluster} client.
 * <p>
 * TODOs: Consider of the <code>keyPrefix</code>, pay attention to the key operation of each method.
 *
 * @author wangnan
 * @see redis.clients.jedis.JedisCluster
 * @since 1.1
 */
public class JedisCache<K, V> implements Cache<K, V> {


    private static final Logger logger = LoggerFactory.getLogger(JedisCache.class);

    /**
     * String Serialization
     */
    private static final Serializer<String> STRING_SERIALIZER = new StringSerializer();

    /**
     * JDK Serialization(not assigned to any ClassLoader)
     */
    private static final Serializer<Object> JDK_SERIALIZATION_SERIALIZER = new JdkSerializationSerializer();

    /**
     * Default Serialization
     */
    private static final Serializer<?> DEFAULT_SERIALIZER = JDK_SERIALIZATION_SERIALIZER;

    /**
     * Cache Name
     */
    private String name;

    /**
     * Cache key prefix
     */
    private byte[] keyPrefix;

    private JedisCluster jedisCluster;

    private Serializer keySerializer = DEFAULT_SERIALIZER;
    private Serializer valueSerializer = DEFAULT_SERIALIZER;
    private Serializer hashKeySerializer = DEFAULT_SERIALIZER;
    private Serializer hashValueSerializer = DEFAULT_SERIALIZER;

    public JedisCache(JedisCluster jedisCluster) {
        this(null, null, jedisCluster);
    }

    public JedisCache(String name,
                      JedisCluster jedisCluster) {
        this(name, null, jedisCluster);
    }

    public JedisCache(byte[] keyPrefix, JedisCluster jedisCluster) {
        this(null, keyPrefix, jedisCluster);
    }

    public JedisCache(String name,
                      byte[] keyPrefix,
                      JedisCluster jedisCluster) {
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.jedisCluster = jedisCluster;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public long del(K... keys) {
        if (keys.length == 1) {
            return jedisCluster.del(rawKey(keys[0]));
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
        long result = 0;
        for (List<byte[]> rawKeys : slotKeyMap.values()) {
            result += jedisCluster.del(rawKeys.toArray(new byte[rawKeys.size()][]));
        }
        return result;
    }

    @Override
    public void set(K key, V value, long seconds) {
        jedisCluster.setex(rawKey(key), (int) seconds, rawValue(value));
    }

    @Override
    public void set(K key, V value, long liveTime, TimeUnit timeUnit) {
        if (liveTime <= 0) {// never expires, regard redis as a DB
            jedisCluster.set(rawKey(key), rawValue(value));
            return;
        }

        if (timeUnit == null) {
            throw new IllegalArgumentException("the TimeUnit of liveTime cannot be null");
        }

        if (Objects.equals(TimeUnit.SECONDS, timeUnit)) {
            jedisCluster.setex(rawKey(key), (int) liveTime, rawValue(value));
        } else if (Objects.equals(TimeUnit.MILLISECONDS, timeUnit)
                && key instanceof String
                && value instanceof String) {
            String completedKey = (String) key;
            if (keyPrefix != null) {
                try {
                    completedKey = new String(keyPrefix) + completedKey;
                } catch (Exception e) {
                    throw new IllegalArgumentException("keyPrefix is not type of String when the key is", e);
                }
            }
            jedisCluster.psetex(completedKey, liveTime, (String) value);// special handle
        } else {
            throw new IllegalArgumentException("invalid TimeUnit or Type of key and value");
        }
    }

    @Override
    public String set(K key, V value, SetOption setOption, long expirationTime, TimeUnit timeUnit) {
        if (setOption == null)
            throw new IllegalArgumentException("set option cannot be null");
        if (expirationTime <= 0)
            throw new IllegalArgumentException("expiration time cannot be zero or negative");

        Expiration expiration = Expiration.from(expirationTime, timeUnit);
        return jedisCluster.set(rawKey(key), rawValue(value), rawString(setOption.getName()), rawString(expiration.toSetCommandExPxArgument()), expiration.getExpirationTime());
    }

    @Override
    public V get(K key) {
        return deserializeValue(jedisCluster.get(rawKey(key)));
    }

    @Override
    @Deprecated
    public Set<K> keys(String pattern) {
        throw new UnsupportedOperationException("Unsupported Command Operation");
    }

    @Override
    @Deprecated
    public List<K> scan(String pattern, long count) {
        throw new UnsupportedOperationException("Unsupported Command Operation");
    }

    @Override
    public ScanResult<String> scan(String cursor, String pattern, long count) {
        ScanParams scanParams = new ScanParams()
                .match(pattern)
                .count((int) count);
        redis.clients.jedis.ScanResult<String> scanResult = jedisCluster.scan(cursor, scanParams);
        return new ScanResult<>(scanResult.getCursorAsBytes(), scanResult.getResult());
    }

    @Override
    public boolean exists(K key) {
        return jedisCluster.exists(rawKey(key));
    }

    @Override
    public boolean expire(K key, long seconds) {
        return jedisCluster.expire(rawKey(key), (int) seconds).equals(1L);
    }

    @Override
    public long ttl(K key) {
        return jedisCluster.ttl(rawKey(key));
    }

    @Override
    public Long incr(K key) {
        return jedisCluster.incr(rawKey(key));
    }

    @Override
    public Long incrBy(K key, long integer) {
        return jedisCluster.incrBy(rawKey(key), integer);
    }

    @Override
    public Long decr(K key) {
        return jedisCluster.decr(rawKey(key));
    }

    @Override
    public Long decrBy(K key, long integer) {
        return jedisCluster.decrBy(rawKey(key), integer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String rename(K oldKey, K newKey) {
        return rename(oldKey, newKey, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String rename(K oldKey, K newKey, RedisConstant.Operation operation) {
        if (JedisClusterCRC16.getSlot(rawKey(oldKey)) == JedisClusterCRC16.getSlot(rawKey(newKey))) {
            return jedisCluster.rename(rawKey(oldKey), rawKey(newKey));
        }

        if (operation == RedisConstant.Operation.HASH) {
            String response = jedisCluster.hmset(rawKey(newKey), jedisCluster.hgetAll(rawKey(oldKey)));
            if (Objects.equals(response, "OK")) {
                expire(newKey, ttl(oldKey));
                del(oldKey);
            }
            return response;
        }
        V value = get(oldKey);
        if (value != null && rawValue(value).length > 0) {
            set(newKey, value, ttl(oldKey));
            del(oldKey);
            return "OK";
        }

        return null;
    }


    @Override
    public Long append(K key, V value) {
        return jedisCluster.append(rawKey(key), rawValue(value));
    }

    @Override
    @Deprecated
    public void flushDB() {
        throw new UnsupportedOperationException("Unsupported Command Operation");
    }

    @Override
    @Deprecated
    public long dbSize() {
        throw new UnsupportedOperationException("Unsupported Command Operation");
    }

    @Override
    @Deprecated
    public String ping() {
        throw new UnsupportedOperationException("Unsupported Command Operation");
    }

    @Override
    public <HK, HV> boolean hSet(K key, HK field, HV value) {
        Long result = jedisCluster.hset(rawKey(key), rawHashKey(field), rawHashValue(value));
        return result.equals(0L) || result.equals(1L);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <HK, HV> HV hGet(K key, HK field) {
        return (HV) deserializeHashValue(jedisCluster.hget(rawKey(key), rawHashKey(field)));
    }

    @Override
    public <HK> Boolean hExists(K key, HK field) {
        return jedisCluster.hexists(rawKey(key), rawHashKey(field));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <HK> Set<HK> hKeys(K key) {
        Set<byte[]> hvBytes = jedisCluster.hkeys(rawKey(key));
        return SerializationUtils.deserialize(hvBytes, hashKeySerializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <HK> long hDel(K key, HK... fields) {
        byte[][] fieldBytes = new byte[fields.length][];
        int index = 0;
        for (HK field : fields) {
            fieldBytes[index++] = rawHashKey(field);
        }
        return jedisCluster.hdel(rawKey(key), fieldBytes);
    }

    @Override
    public void hMSet(K key, Map<String, Object> hashes) {
        Map<byte[], byte[]> hashBytes = new HashMap<>();
        for (Map.Entry<String, Object> entry : hashes.entrySet()) {
            hashBytes.put(rawHashKey(entry.getKey()), rawHashValue(entry.getValue()));
        }
        jedisCluster.hmset(rawKey(key), hashBytes);
    }

    @Override
    public <HK, HV> Map<HK, HV> hGetAll(K key) {
        Map<HK, HV> result = new HashMap<>();
        Map<byte[], byte[]> hashes = jedisCluster.hgetAll(rawKey(key));
        for (Map.Entry<byte[], byte[]> entry : hashes.entrySet()) {
            result.put(deserializeHashKey(entry.getKey()), deserializeHashValue(entry.getValue()));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public long lPush(K key, V... values) {
        byte[][] valueBytes = new byte[values.length][];
        int index = 0;
        for (Object value : values) {
            valueBytes[index++] = rawValue(value);
        }
        return jedisCluster.lpush(rawKey(key), valueBytes);
    }

    @Override
    public List<V> lRange(K key, long begin, long end) {
        List<byte[]> resultBytes = jedisCluster.lrange(rawKey(key), begin, end);
        List<V> vs = new ArrayList<>();
        resultBytes.forEach(resultByte -> vs.add(deserializeValue(resultByte)));
        return vs;
    }

    @Override
    public long lRem(K key, long count, V value) {
        return jedisCluster.lrem(rawKey(key), count, rawValue(value));
    }

    @Override
    public void lTrim(K key, long begin, long end) {
        jedisCluster.ltrim(rawKey(key), begin, end);
    }

    @Override
    public V lPop(K key) {
        return deserializeValue(jedisCluster.lpop(rawKey(key)));
    }

    @Override
    public long lLen(K key) {
        return jedisCluster.llen(rawKey(key));
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        return jedisCluster.eval(script, keyCount, params);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawKey(Object key) {
        if (keyPrefix != null) {
            return new RedisCacheKey(key).usePrefix(keyPrefix).withKeySerializer(getKeySerializer()).getKeyBytes();
        }

        return getKeySerializer().serialize(key);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawValue(Object value) {
        return getValueSerializer().serialize(value);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawHashKey(Object key) {
        return getHashKeySerializer().serialize(key);
    }

    @SuppressWarnings("unchecked")
    private byte[] rawHashValue(Object value) {
        return getHashValueSerializer().serialize(value);
    }

    @SuppressWarnings("unchecked")
    private V deserializeValue(byte[] value) {
        return (V) getValueSerializer().deserialize(value);
    }

    @SuppressWarnings("unchecked")
    private <HK> HK deserializeHashKey(byte[] hk) {
        return (HK) getHashKeySerializer().deserialize(hk);
    }

    @SuppressWarnings("unchecked")
    private <HV> HV deserializeHashValue(byte[] hv) {
        return (HV) getHashValueSerializer().deserialize(hv);
    }

    private byte[] rawString(String string) {
        return STRING_SERIALIZER.serialize(string);
    }

    public Serializer getKeySerializer() {
        return keySerializer;
    }

    public Serializer getValueSerializer() {
        return valueSerializer;
    }

    public Serializer getHashKeySerializer() {
        return hashKeySerializer;
    }

    public Serializer getHashValueSerializer() {
        return hashValueSerializer;
    }

    public void setKeySerializer(Serializer keySerializer) {
        this.keySerializer = keySerializer;
    }

    public void setValueSerializer(Serializer valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public void setHashKeySerializer(Serializer hashKeySerializer) {
        this.hashKeySerializer = hashKeySerializer;
    }

    public void setHashValueSerializer(Serializer hashValueSerializer) {
        this.hashValueSerializer = hashValueSerializer;
    }

    /**
     * This method should be invoked every time you use it up,
     * but you can do multiple operations every time you use it.
     */
    @Override
    public void close() throws IOException {
        try {
            if (jedisCluster != null)
                jedisCluster.close();
        } catch (IOException e) {
            logger.error("jedisCluster close failed", e);
        }
    }

    public JedisCluster getNativeJedisCluster() {
        return jedisCluster;
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
