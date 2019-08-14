package com.caiya.cache.redis.springx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import com.caiya.cache.CacheException;
import com.caiya.cache.redis.JedisCache;
import com.caiya.cache.redis.JedisClusterConnection;
import com.caiya.cache.redis.RedisConnection;
import com.caiya.cache.redis.RedisConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

/**
 * {@link JedisCacheWriter} implementation capable of reading/writing binary data from/to Redis in {@literal standalone}
 * and {@literal cluster} environments. Works upon a given {@link RedisConnectionFactory} to obtain the actual
 * {@link RedisConnection}. <br />
 * {@link DefaultJedisCacheWriter} can be used in
 * {@link JedisCacheWriter#lockingRedisCacheWriter(RedisConnectionFactory) locking} or
 * {@link JedisCacheWriter#nonLockingRedisCacheWriter(RedisConnectionFactory) non-locking} mode. While
 * {@literal non-locking} aims for maximum performance it may result in overlapping, non atomic, command execution for
 * operations spanning multiple Redis interactions like {@code putIfAbsent}. The {@literal locking} counterpart prevents
 * command overlap by setting an explicit lock key and checking against presence of this key which leads to additional
 * requests and potential command wait times.
 *
 * @author wangnan
 * @since 1.1.1
 */
class DefaultJedisCacheWriter implements JedisCacheWriter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultJedisCacheWriter.class);

    private final RedisConnectionFactory connectionFactory;
    private final Duration sleepTime;

    /**
     * @param connectionFactory must not be {@literal null}.
     */
    DefaultJedisCacheWriter(RedisConnectionFactory connectionFactory) {
        this(connectionFactory, Duration.ZERO);
    }

    /**
     * @param connectionFactory must not be {@literal null}.
     * @param sleepTime         sleep time between lock request attempts. Must not be {@literal null}. Use {@link Duration#ZERO}
     *                          to disable locking.
     */
    DefaultJedisCacheWriter(RedisConnectionFactory connectionFactory, Duration sleepTime) {

        Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");
        Assert.notNull(sleepTime, "SleepTime must not be null!");

        this.connectionFactory = connectionFactory;
        this.sleepTime = sleepTime;
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public void put(String name, byte[] key, byte[] value, Duration ttl) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");

        execute(name, connection -> {

            if (shouldExpireWithin(ttl)) {
                connection.getNativeJedisCluster().setex(key, (int) (ttl.toMillis() / 1000), value);
            } else {
                connection.getNativeJedisCluster().set(key, value);
            }

            return "OK";
        });
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public byte[] get(String name, byte[] key) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");

        return execute(name, connection -> connection.getNativeJedisCluster().get(key));
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");

        return execute(name, connection -> {

            if (isLockingCacheWriter()) {
                doLock(name, connection);
            }

            try {
                if (connection.getNativeJedisCluster().setnx(key, value) != -1) {

                    if (shouldExpireWithin(ttl)) {
                        connection.getNativeJedisCluster().pexpire(key, ttl.toMillis());
                    }
                    return null;
                }

                return connection.getNativeJedisCluster().get(key);
            } finally {

                if (isLockingCacheWriter()) {
                    doUnlock(name, connection);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.redis.cache.JedisCacheWriter#remove(java.lang.String, byte[])
     */
    @Override
    public void remove(String name, byte[] key) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");

        execute(name, connection -> connection.getNativeJedisCluster().del(key));
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public void clean(String name, byte[] pattern) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(pattern, "Pattern must not be null!");

        execute(name, connection -> {

            boolean wasLocked = false;

            try {

                if (isLockingCacheWriter()) {
                    doLock(name, connection);
                    wasLocked = true;
                }

                // 循环情况key
                String scanCursor = "0";
                int batchMaxSize = 100;
                do {
                    ScanParams scanParams = new ScanParams()
                            .match(pattern)
                            .count(batchMaxSize);
                    ScanResult<String> scanResult = connection.getNativeJedisCluster().scan(scanCursor, scanParams);
                    scanCursor = scanResult.getStringCursor();
                    if (scanResult.getResult().size() > 0) {
                        connection.getNativeJedisCluster().del(scanResult.getResult().toArray(new String[0]));
                    } else {
                        break;
                    }
                } while (!scanCursor.equals("0"));
            } finally {

                if (wasLocked && isLockingCacheWriter()) {
                    doUnlock(name, connection);
                }
            }

            return "OK";
        });
    }

    /**
     * Explicitly set a write lock on a cache.
     *
     * @param name the name of the cache to lock.
     */
    void lock(String name) {
        execute(name, connection -> doLock(name, connection));
    }

    /**
     * Explicitly remove a write lock from a cache.
     *
     * @param name the name of the cache to unlock.
     */
    void unlock(String name) {
        executeLockFree(connection -> doUnlock(name, connection));
    }

    private Boolean doLock(String name, JedisCache connection) {
        return connection.getNativeJedisCluster().setnx(createCacheLockKey(name), new byte[0]) != -1;
    }

    private Long doUnlock(String name, JedisCache connection) {
        return connection.getNativeJedisCluster().del(createCacheLockKey(name));
    }

    boolean doCheckLock(String name, JedisCache connection) {
        return connection.getNativeJedisCluster().exists(createCacheLockKey(name));
    }

    /**
     * @return {@literal true} if {@link JedisCacheWriter} uses locks.
     */
    private boolean isLockingCacheWriter() {
        return !sleepTime.isZero() && !sleepTime.isNegative();
    }

    private <T> T execute(String name, Function<JedisCache, T> callback) {

        JedisCache connection = ((JedisClusterConnection) (connectionFactory.getConnection())).getNativeConnection();
        try {

            checkAndPotentiallyWaitUntilUnlocked(name, connection);
            return callback.apply(connection);
        } finally {
//            try {
//                connection.close();
//            } catch (IOException e) {
//                logger.error(e.getMessage(), e);
//            }
        }
    }

    private void executeLockFree(Consumer<JedisCache> callback) {

        JedisCache connection = ((JedisClusterConnection) (connectionFactory.getConnection())).getNativeConnection();

        try {
            callback.accept(connection);
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void checkAndPotentiallyWaitUntilUnlocked(String name, JedisCache connection) {

        if (!isLockingCacheWriter()) {
            return;
        }

        try {

            while (doCheckLock(name, connection)) {
                Thread.sleep(sleepTime.toMillis());
            }
        } catch (InterruptedException ex) {

            // Re-interrupt current thread, to allow other participants to react.
            Thread.currentThread().interrupt();

            throw new CacheException(String.format("Interrupted while waiting to unlock cache %s", name),
                    ex);
        }
    }

    private static boolean shouldExpireWithin(Duration ttl) {
        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }

    private static byte[] createCacheLockKey(String name) {
        return (name + "~lock").getBytes(StandardCharsets.UTF_8);
    }
}
