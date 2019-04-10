package com.caiya.cache.redis.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Abstract class for Lock Implementations.
 *
 * @author wangnan
 * @since 1.0
 */
public abstract class AbstractLock<T> implements Lock<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final long DEFAULT_LOCK_TIME_SECONDS = 20;

    private static final long DEFAULT_MAX_WAIT_SECONDS = 30;

    /**
     * 锁的名字
     */
    private String name;

    /**
     * 锁的对象
     */
    private T target;

    /**
     * 锁的操作时间,防止造成死锁
     */
    private Duration lockTimeout = Duration.ofSeconds(DEFAULT_LOCK_TIME_SECONDS);

    /**
     * 等待锁定的最大尝试时间(之前用最大尝试次数和每两次尝试间隔时间指定)
     */
    private Duration maxWaitTime = Duration.ofSeconds(DEFAULT_MAX_WAIT_SECONDS);

    /**
     * Integration method for easy to use.
     *
     * @param callBack callback handler
     * @param <R>      the type of success result
     * @return the success result
     */
    protected <R> R execute(LockCallBack<R> callBack) {
        if (callBack == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }

        boolean locked = lock();
        if (locked) {
            try {
                return callBack.onSuccess();
            } finally {
                unlock();
            }
        }

        callBack.onFailure(new RuntimeException("redis lock failed"));
        return null;
    }


    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    protected void setTarget(T target) {
        this.target = target;
    }

    @Override
    public T getTarget() {
        return target;
    }

    public void setLockTimeout(Duration lockTimeout) {
        if (lockTimeout != null)
            this.lockTimeout = lockTimeout;
    }

    public void setMaxWaitTime(Duration maxWaitTime) {
        if (maxWaitTime != null)
            this.maxWaitTime = maxWaitTime;
    }

    @Override
    public Duration getLockTimeout() {
        return lockTimeout;
    }

    @Override
    public Duration getMaxWaitTime() {
        return maxWaitTime;
    }
}
