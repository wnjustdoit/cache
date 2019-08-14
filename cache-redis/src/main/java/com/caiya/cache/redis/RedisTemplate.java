package com.caiya.cache.redis;

import com.caiya.cache.RedisConstant;
import com.caiya.cache.ScanResult;
import com.caiya.cache.SetOption;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Helper class that simplifies Redis data access code.
 * <p>
 * Once configured, this class is thread-safe.
 * <p>
 * <b>This is the central class in Redis support</b>.
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisTemplate<K, V> extends RedisAccessor<K, V> implements RedisOperations<K, V> {

    private volatile boolean initialized;

    public RedisTemplate() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() {
        super.afterPropertiesSet();

        initialized = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R execute(RedisCallback<R, K, V> action) {
        if (action == null) {
            throw new IllegalArgumentException("Callback object must not be null");
        }
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    afterPropertiesSet();
                    initialized = true;
                }
            }
        }

        RedisConnectionFactory factory = getConnectionFactory();
        RedisConnection connection = null;
        try {
            connection = factory.getConnection();
            return action.doInRedis((JedisCache<K, V>) ((JedisClusterConnection) connection).getNativeConnection());
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (Exception e) {
                logger.error("redis client close failed", e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public long del(K... keys) {
        return execute(jedisCache -> jedisCache.del(keys));
    }

    @Override
    public void set(K key, V value, long seconds) {
        execute(redisCache -> {
            redisCache.set(key, value, seconds);
            return null;
        });
    }

    @Override
    public void set(K key, V value, long liveTime, TimeUnit timeUnit) {
        execute(redisCache -> {
            redisCache.set(key, value, liveTime, timeUnit);
            return null;
        });
    }

    @Override
    public String set(K key, V value, SetOption setOption, long expirationTime, TimeUnit timeUnit) {
        return execute(redisCache -> redisCache.set(key, value, setOption, expirationTime, timeUnit));
    }

    @Override
    public V get(K key) {
        return execute(redisCache -> redisCache.get(key));
    }

    @Override
    @Deprecated
    public Set<K> keys(String pattern) {
        return execute(redisCache -> redisCache.keys(pattern));
    }

    @Override
    @Deprecated
    public List<K> scan(String pattern, long count) {
        return execute(redisCache -> redisCache.scan(pattern, count));
    }

    @Override
    public ScanResult<String> scan(String cursor, String pattern, long count) {
        return execute(redisCache -> redisCache.scan(cursor, pattern, count));
    }

    @Override
    public boolean exists(K key) {
        return execute(redisCache -> redisCache.exists(key));
    }

    @Override
    public boolean expire(K key, long seconds) {
        return execute(redisCache -> redisCache.expire(key, seconds));
    }

    @Override
    public long ttl(K key) {
        return execute(redisCache -> redisCache.ttl(key));
    }

    @Override
    public Long incr(K key) {
        return execute(redisCache -> redisCache.incr(key));
    }

    @Override
    public Long incrBy(K key, long integer) {
        return execute(redisCache -> redisCache.incrBy(key, integer));
    }

    @Override
    public Long decr(K key) {
        return execute(redisCache -> redisCache.decr(key));
    }

    @Override
    public Long decrBy(K key, long integer) {
        return execute(redisCache -> redisCache.decrBy(key, integer));
    }

    @Override
    public String rename(K oldKey, K newKey) {
        return execute(redisCache -> redisCache.rename(oldKey, newKey));
    }

    @Override
    public String rename(K oldKey, K newKey, RedisConstant.Operation operation) {
        return execute(redisCache -> redisCache.rename(oldKey, newKey, operation));
    }

    @Override
    public Long append(K key, V value) {
        return execute(redisCache -> redisCache.append(key, value));
    }

    @Override
    @Deprecated
    public void flushDB() {
        execute(redisCache -> {
            redisCache.flushDB();
            return null;
        });
    }

    @Override
    @Deprecated
    public long dbSize() {
        return execute(JedisCache::dbSize);
    }

    @Override
    @Deprecated
    public String ping() {
        return execute(JedisCache::ping);
    }

    @Override
    public <HK, HV> boolean hSet(K key, HK field, HV value) {
        return execute(redisCache -> redisCache.hSet(key, field, value));
    }

    @Override
    public <HK, HV> HV hGet(K key, HK field) {
        return execute(redisCache -> redisCache.hGet(key, field));
    }

    @Override
    public <HK> Boolean hExists(K key, HK field) {
        return execute(redisCache -> redisCache.hExists(key, field));
    }

    @Override
    public <HK> Set<HK> hKeys(K key) {
        return execute(redisCache -> redisCache.hKeys(key));
    }

    @SafeVarargs
    @Override
    public final <HK> long hDel(K key, HK... fields) {
        return execute(redisCache -> redisCache.hDel(key, fields));
    }

    @Override
    public void hMSet(K key, Map<String, Object> hashes) {
        execute(redisCache -> {
            redisCache.hMSet(key, hashes);
            return null;
        });
    }

    @Override
    public <HK, HV> Map<HK, HV> hGetAll(K key) {
        return execute(redisCache -> redisCache.hGetAll(key));
    }

    @SafeVarargs
    @Override
    public final long lPush(K key, V... values) {
        return execute(redisCache -> redisCache.lPush(key, values));
    }

    @Override
    public List<V> lRange(K key, long begin, long end) {
        return execute(redisCache -> redisCache.lRange(key, begin, end));
    }

    @Override
    public long lRem(K key, long count, V value) {
        return execute(redisCache -> redisCache.lRem(key, count, value));
    }

    @Override
    public void lTrim(K key, long begin, long end) {
        execute(redisCache -> {
            redisCache.lTrim(key, begin, end);
            return null;
        });
    }

    @Override
    public V lPop(K key) {
        return execute(redisCache -> redisCache.lPop(key));
    }

    @Override
    public long lLen(K key) {
        return execute(redisCache -> redisCache.lLen(key));
    }

    @Override
    public Object eval(String script, int keyCount, String... params) {
        return execute(redisCache -> redisCache.eval(script, keyCount, params));
    }

    @Override
    public byte[] getKeyPrefix() {
        return ((JedisConnectionFactory) getConnectionFactory()).getKeyPrefix();
    }

}
