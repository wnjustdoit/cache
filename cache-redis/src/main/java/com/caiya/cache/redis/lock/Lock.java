package com.caiya.cache.redis.lock;

import java.time.Duration;

/**
 * Lock Interface.
 *
 * @author wangnan
 * @since 1.0
 */
public interface Lock<T> {

    /**
     * @return The lock's name.
     */
    String getName();

    /**
     * @return lock target.it may be a java object or a key of redis.
     */
    T getTarget();

    /**
     * Core method of lock, lock a object that has to be.
     *
     * @return the lock result, true or false.
     */
    boolean lock();

    /**
     * Try lock only once.
     *
     * @return try result,true or false.
     */
    boolean tryLockOnce();

    /**
     * Try lock in the duration time.
     *
     * @param duration max wait time.
     * @return try result,true or false.
     */
    boolean tryLock(Duration duration);

    /**
     * Release the lock,usually the lock of current thread.
     */
    void unlock();

    /**
     * @return The timeout of this lock.
     */
    Duration getLockTimeout();

    /**
     * @return Max wait time during try to lock.
     */
    Duration getMaxWaitTime();
}
