package com.caiya.cache;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Cache Api.
 *
 * @author wangnan
 * @since 1.0
 */
public interface CacheApi<K, V> {


    /**
     * Delete given {@code keys}.
     * Compatibility for cluster mode.
     * <p>
     * See http://redis.io/commands/del
     *
     * @param keys the cache keys
     * @return The number of keys that were removed.
     */
    @SuppressWarnings("unchecked")
    long del(K... keys);

    /**
     * Set the {@code value} and expiration in {@code seconds} for {@code key}.
     * May be deprecated in the future, recommended to use {@link #set(K key, V value, long liveTime, TimeUnit timeUnit);} instead.
     * <p>
     * See http://redis.io/commands/setex
     *
     * @param key     the cache key
     * @param seconds live time
     * @param value   the cache value
     */
    void set(K key, V value, long seconds);

    /**
     * Set the {@code value} and expiration in {@code milliseconds} for {@code key}.
     * <p>
     * See http://redis.io/commands/set
     * See http://redis.io/commands/setex
     * See http://redis.io/commands/psetex
     *
     * @param key      the cache key
     * @param value    the cache value
     * @param liveTime live time
     * @param timeUnit the TimeUnit of live time
     */
    void set(K key, V value, long liveTime, TimeUnit timeUnit);

    /**
     * Starting with Redis 2.6.12 SET supports a set of options that modify its behavior:
     * <p>
     * EX seconds -- Set the specified expire time, in seconds.
     * PX milliseconds -- Set the specified expire time, in milliseconds.
     * NX -- Only set the key if it does not already exist.
     * XX -- Only set the key if it already exist.
     * Note: Since the SET command options can replace SETNX, SETEX, PSETEX, it is possible that in future versions of Redis these three commands will be deprecated and finally removed.
     * See https://redis.io/commands/set
     *
     * @param key            the cache key
     * @param value          the cache value
     * @param setOption      "nxxx" options
     * @param expirationTime liveTime
     * @param timeUnit       the timeunit of liveTime
     * @return response
     * @throws CacheException
     */
    String set(K key, V value, SetOption setOption, long expirationTime, TimeUnit timeUnit);

    /**
     * Get the value of {@code key}.
     * <p>
     * See http://redis.io/commands/get
     *
     * @param key the cache key
     * @return the cache value
     */
    V get(K key);

    /**
     * Find all keys matching the given {@code pattern}.
     * <p>
     * See http://redis.io/commands/keys
     *
     * @param pattern the cache key's pattern
     * @return key set
     */
    @Deprecated
    Set<K> keys(String pattern);

    /**
     * Use a cursor to iterate over keys.
     * <p>
     * See http://redis.io/commands/scan
     *
     * @param pattern the cache key's pattern
     * @param count   count of number
     * @return key list
     * @since redis v2.8
     */
    @Deprecated
    List<K> scan(String pattern, long count);

    /**
     * Use a cursor to iterate over keys.
     * Compatibility for cluster mode.
     * Only supported hashtag Key in JedisCluster.
     * <p>
     * See http://redis.io/commands/scan
     *
     * @param cursor  the cursor
     * @param pattern the key's pattern
     * @param count   count of number
     * @return result map
     */
    ScanResult<String> scan(String cursor, String pattern, long count);

    /**
     * Determine if given {@code key} exists.
     * <p>
     * See http://redis.io/commands/exists
     *
     * @param key the cache key
     * @return if exists
     */
    boolean exists(K key);

    /**
     * Set time to live for given {@code key} in seconds.
     * <p>
     * See http://redis.io/commands/expire
     *
     * @param key     the cache key
     * @param seconds liveTime
     * @return if success
     */
    boolean expire(K key, long seconds);

    /**
     * Get the time to live for {@code key} in seconds.
     * <p>
     * See http://redis.io/commands/ttl
     *
     * @param key the cache key
     * @return liveTime
     */
    long ttl(K key);

    /**
     * Rename key {@code oldKey} to {@code newKey}.
     * Compatibility for cluster mode.
     * Attention the RedisCache implement is not strict, as it does not copy the ttl time.
     *
     * @param oldKey must not be {@literal null}.
     * @param newKey must not be {@literal null}.
     * @see <a href="http://redis.io/commands/rename">Redis Documentation: RENAME</a>
     */
    String rename(K oldKey, K newKey);

    /**
     * Rename key {@code oldKey} to {@code newKey}.
     * Compatibility for cluster mode.
     * Attention the RedisCache implement is not strict, as it does not copy the ttl time.
     *
     * @param oldKey    must not be {@literal null}.
     * @param newKey    must not be {@literal null}.
     * @param operation can be {@literal null}
     * @see <a href="http://redis.io/commands/rename">Redis Documentation: RENAME</a>
     */
    String rename(K oldKey, K newKey, RedisConstant.Operation operation);

    /**
     * Append a {@code value} to {@code key}.
     *
     * @param key   must not be {@literal null}.
     * @param value the append value
     * @return length of bytes, it depends on what the value serializer is.
     * @see <a href="http://redis.io/commands/append">Redis Documentation: APPEND</a>
     */
    Long append(K key, V value);

    /**
     * Delete all keys of the currently selected database.
     * <p>
     * See http://redis.io/commands/flushdb
     */
    void flushDB();

    /**
     * Get the total number of available keys in currently selected database.
     * <p>
     * See http://redis.io/commands/dbsize
     *
     * @return the number of keys
     */
    long dbSize();

    /**
     * Test connection.
     * <p>
     * See http://redis.io/commands/ping
     *
     * @return Server response message - usually {@literal PONG}.
     */
    String ping();

    /**
     * Set the {@code value} of a hash {@code field}.
     *
     * @param key   the cache key
     * @param field the hash field
     * @param value the hash value
     * @return See http://redis.io/commands/hset
     */
    <HK, HV> boolean hSet(K key, HK field, HV value);

    /**
     * Get value for given {@code field} from hash at {@code key}.
     * <p>
     * See http://redis.io/commands/hget
     *
     * @param key   the cache key
     * @param field the hash field
     * @return the hash value
     */
    <HK, HV> HV hGet(K key, HK field);

    /**
     * Determine if given hash {@code field} exists.
     *
     * @param key   must not be {@literal null}.
     * @param field must not be {@literal null}.
     * @return if hash field exists
     * @see <a href="http://redis.io/commands/hexits">Redis Documentation: HEXISTS</a>
     */
    <HK> Boolean hExists(K key, HK field);

    /**
     * Get key set (fields) of hash at {@code key}.
     * <p>
     * See http://redis.io/commands/h?
     *
     * @param key the cache key
     * @return the hash fields
     */
    <HK> Set<HK> hKeys(K key);

    /**
     * Delete given hash {@code fields}.
     *
     * @param key    the cache key
     * @param fields the hash fields to delete
     * @return See http://redis.io/commands/hdel
     */
    @SuppressWarnings("unchecked")
    <HK> long hDel(K key, HK... fields);

    /**
     * Set multiple hash fields to multiple values using data provided in {@code hashes}
     *
     * @param key    the cache key
     * @param hashes See http://redis.io/commands/hmset
     */
    void hMSet(K key, Map<String, Object> hashes);

    /**
     * Get entire hash stored at {@code key}.
     *
     * @param key must not be {@literal null}.
     * @see <a href="http://redis.io/commands/hgetall">Redis Documentation: HGETALL</a>
     */
    <HK, HV> Map<HK, HV> hGetAll(K key);

    /**
     * Prepend {@code values} to {@code key}.
     * <p>
     * See http://redis.io/commands/lpush
     *
     * @param key    the cache key
     * @param values the values
     * @return See the usage
     */
    @SuppressWarnings("unchecked")
    long lPush(K key, V... values);

    /**
     * Get elements between {@code begin} and {@code end} from list at {@code key}.
     * <p>
     * See http://redis.io/commands/lrange
     *
     * @param key   the cache key
     * @param begin the begin index
     * @param end   the end index
     * @return See the usage
     */
    List<V> lRange(K key, long begin, long end);

    /**
     * Removes the first {@code count} occurrences of {@code value} from the list stored at {@code key}.
     * <p>
     * See http://redis.io/commands/lrem
     *
     * @param key   the cache key
     * @param count count
     * @param value the value
     * @return See the usage
     */
    long lRem(K key, long count, V value);

    /**
     * Trim list at {@code key} to elements between {@code begin} and {@code end}.
     * <p>
     * See http://redis.io/commands/ltrim
     *
     * @param key   the cache key
     * @param begin the begin index
     * @param end   the end index
     */
    void lTrim(K key, long begin, long end);

    /**
     * Removes and returns first element in list stored at {@code key}.
     * <p>
     * See http://redis.io/commands/lpop
     *
     * @param key the cache key
     * @return the value
     */
    V lPop(K key);

    /**
     * Get the size of list stored at {@code key}.
     * <p>
     * See http://redis.io/commands/llen
     *
     * @param key the cache key
     * @return list's size
     */
    long lLen(K key);

    /**
     * Use LUA Script to calc value.
     *
     * @param script   the lua script
     * @param keyCount keys' count
     * @param params   params include keys and values
     * @return response
     */
    Object eval(String script, int keyCount, String... params);

    /**
     * @return Cache key prefix.
     */
    byte[] getKeyPrefix();

}
