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
     * Acquires the lock.
     */
    void lock();

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     *
     * @throws InterruptedException if the current thread is
     *                              interrupted while acquiring the lock (and interruption
     *                              of lock acquisition is supported)
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires the lock only if it is free at the time of invocation.
     *
     * @return {@code true} if the lock was acquired and {@code false} otherwise
     */
    boolean tryLock();

    /**
     * Acquires the lock if it is free within the given waiting time and the
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * @param duration max wait time.
     * @return {@code true} if the lock was acquired and {@code false} if the waiting time elapsed before the lock was acquired
     * @throws InterruptedException if the current thread is interrupted
     *                              while acquiring the lock (and interruption of lock
     *                              acquisition is supported)
     */
    boolean tryLock(Duration duration) throws InterruptedException;

    /**
     * Releases the lock.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation will usually impose
     * restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     */
    void unlock();

    /**
     * @return The lock's name.
     */
    String getName();

    /**
     * @return The timeout of this lock.
     */
    Duration getLockTimeout();

}
