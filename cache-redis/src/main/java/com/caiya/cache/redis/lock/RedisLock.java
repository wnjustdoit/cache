package com.caiya.cache.redis.lock;

import com.caiya.cache.CacheApi;
import com.caiya.cache.SetOption;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Distributed lock based-on redis.
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisLock extends AbstractLock<String> {

    /**
     * The cache client
     */
    private final CacheApi<String, String> cache;


    public RedisLock(CacheApi<String, String> cache, String name) {
        if (cache == null) {
            throw new IllegalArgumentException("the cache client cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("the lock name cannot be null");
        }

        this.cache = cache;
        super.setName(name);
    }

    public RedisLock(CacheApi<String, String> cache, String name, Duration lockTimeout) {
        this(cache, name);
        super.setLockTimeout(lockTimeout);
    }

    @Override
    public void lock() {
        if (!tryLock()) {
            try {
                doAcquireMillis(Duration.ofSeconds(Integer.MAX_VALUE), false);
            } catch (InterruptedException e) {
                // ignore exception
            }
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryLock()) {
            doAcquireMillis(Duration.ofSeconds(Integer.MAX_VALUE), true);
        }
    }

    @Override
    public boolean tryLock() {
        return lockInternal();
    }

    @Override
    public boolean tryLock(Duration maxWaitTime) throws InterruptedException {
        if (maxWaitTime == null) {
            throw new IllegalArgumentException("maxWaitTime cannot be null");
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        return tryLock() || doAcquireMillis(maxWaitTime, true);
    }

    private boolean lockInternal() {
        return "OK".equals(cache.set(getName(), getValue(), SetOption.SET_IF_ABSENT, getLockTimeout().toMillis(), TimeUnit.MILLISECONDS));
    }

    private boolean doAcquireMillis(Duration maxWaitTime, boolean interrupted) throws InterruptedException {
        long millisTimeout;
        if (maxWaitTime == null || (millisTimeout = maxWaitTime.toMillis()) <= 0L) {
            return false;
        }
        final long deadline = System.currentTimeMillis() + millisTimeout;
        boolean failed = true;
        try {
            for (; ; ) {
                if (tryLock()) {
                    failed = false;
                    return true;
                }
                millisTimeout = deadline - System.currentTimeMillis();
                if (millisTimeout <= 0L) {
                    return false;
                }
                if (interrupted && Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }
        } finally {
            if (failed) {

            }
        }
    }

    @Override
    public void unlock() {
        String cacheName = (cache.getKeyPrefix() == null || cache.getKeyPrefix().length == 0)
                ? getName()
                : (new String(cache.getKeyPrefix(), StandardCharsets.UTF_8) + getName());
        cache.eval("if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",
                1, cacheName, getValue());
    }


}
