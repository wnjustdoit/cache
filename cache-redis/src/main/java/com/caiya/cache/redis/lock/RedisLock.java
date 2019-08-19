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


    RedisLock(CacheApi<String, String> cache, String name) {
        if (cache == null) {
            throw new IllegalArgumentException("the cache client cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("the lock name cannot be null");
        }

        this.cache = cache;
        super.setName(name);
    }

    @Override
    public void lock() {
        if (!tryLock()) {
            try {
                doAcquireMillis(getDefaultWaitTime(), getDefaultLeaseTime(), false);
            } catch (InterruptedException e) {
                // ignore exception
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void lock(Duration leaseTime) {
        if (!tryLock()) {
            try {
                doAcquireMillis(getDefaultWaitTime(), leaseTime, false);
            } catch (InterruptedException e) {
                // ignore exception
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryLock()) {
            doAcquireMillis(getDefaultWaitTime(), getDefaultLeaseTime(), true);
        }
    }

    @Override
    public void lockInterruptibly(Duration leaseTime) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        if (!tryLock()) {
            doAcquireMillis(getDefaultWaitTime(), leaseTime, true);
        }
    }

    @Override
    public boolean tryLock() {
        return lockInternal(getDefaultLeaseTime());
    }

    @Override
    public boolean tryLock(Duration waitTime) throws InterruptedException {
        if (waitTime == null) {
            throw new IllegalArgumentException("waitTime cannot be null");
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        return lockInternal(getDefaultLeaseTime()) || doAcquireMillis(waitTime, getDefaultLeaseTime(), true);
    }

    @Override
    public boolean tryLock(Duration waitTime, Duration leaseTime) throws InterruptedException {
        if (waitTime == null) {
            throw new IllegalArgumentException("waitTime cannot be null");
        }
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        return lockInternal(getDefaultLeaseTime()) || doAcquireMillis(waitTime, leaseTime, true);
    }

    private boolean lockInternal(Duration leaseTime) {
        if (leaseTime == null || leaseTime.isZero()) {
            throw new IllegalArgumentException("leaseTime cannot be null or zero");
        }
        return "OK".equals(cache.set(getName(), getValueByThreadId(Thread.currentThread().getId()), SetOption.SET_IF_ABSENT, leaseTime.toMillis(), TimeUnit.MILLISECONDS));
    }

    /**
     * 尝试加锁，以毫秒为精度轮询
     *
     * @param waitTime    max wait time
     * @param leaseTime   max lease time, can be nullable or zero
     * @param interrupted if interrupted or not
     * @return lock result
     * @throws InterruptedException InterruptedException
     */
    private boolean doAcquireMillis(Duration waitTime, Duration leaseTime, boolean interrupted) throws InterruptedException {
        long millisTimeout;
        if (waitTime == null || (millisTimeout = waitTime.toMillis()) <= 0L) {
            return false;
        }
        final long deadline = System.currentTimeMillis() + millisTimeout;
        boolean failed = true;
        try {
            for (; ; ) {
                if (lockInternal(leaseTime)) {
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
                logger.warn("doAcquireMillis failed");
            }
        }
    }

    /**
     * Special handle with key prefix in Lua Script
     */
    @Override
    public void unlock() {
        String cacheName = (cache.getKeyPrefix() == null || cache.getKeyPrefix().length == 0)
                ? getName()
                : (new String(cache.getKeyPrefix(), StandardCharsets.UTF_8) + getName());
        cache.eval("if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",
                1, cacheName, getValueByThreadId(Thread.currentThread().getId()));
    }

    @Override
    public boolean isLocked() {
        return cache.exists(getName());
    }

    @Override
    public boolean isHeldByThread(long threadId) {
        return getValueByThreadId(threadId).equals(cache.get(getName()));
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return isHeldByThread(Thread.currentThread().getId());
    }

    @Override
    public int getHoldCount() {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public Duration remainTimeToLive() {
        return Duration.ofSeconds(cache.ttl(getName()));
    }


}
