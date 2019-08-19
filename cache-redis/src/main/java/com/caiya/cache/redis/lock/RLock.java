package com.caiya.cache.redis.lock;

import java.time.Duration;

/**
 * Redis Lock Interface.
 *
 * @author wangnan
 * @since 1.0
 */
public interface RLock<T> {

    /**
     * Acquires the lock.
     */
    void lock();

    /**
     * Acquires the lock.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     * <p>
     * If the lock is acquired, it is held until <code>unlock</code> is invoked,
     * or until leaseTime milliseconds have passed
     * since the lock was granted - whichever comes first.
     *
     * @param leaseTime the maximum time to hold the lock after granting it,
     *                  before automatically releasing it if it hasn't already been released by invoking <code>unlock</code>.
     *                  If leaseTime is -1, hold the lock until explicitly unlocked.
     */
    void lock(Duration leaseTime);

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
     * Acquires the lock.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     * <p>
     * If the lock is acquired, it is held until <code>unlock</code> is invoked,
     * or until leaseTime have passed
     * since the lock was granted - whichever comes first.
     *
     * @param leaseTime the maximum time to hold the lock after granting it,
     *                  before automatically releasing it if it hasn't already been released by invoking <code>unlock</code>.
     *                  If leaseTime is -1, hold the lock until explicitly unlocked.
     * @throws InterruptedException - if the thread is interrupted before or during this method.
     */
    void lockInterruptibly(Duration leaseTime) throws InterruptedException;

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
     * @param waitTime max wait time.
     * @return {@code true} if the lock was acquired and {@code false} if the waiting time elapsed before the lock was acquired
     * @throws InterruptedException if the current thread is interrupted
     *                              while acquiring the lock (and interruption of lock
     *                              acquisition is supported)
     */
    boolean tryLock(Duration waitTime) throws InterruptedException;

    /**
     * Returns <code>true</code> as soon as the lock is acquired.
     * If the lock is currently held by another thread in this or any
     * other process in the distributed system this method keeps trying
     * to acquire the lock for up to <code>waitTime</code> before
     * giving up and returning <code>false</code>. If the lock is acquired,
     * it is held until <code>unlock</code> is invoked, or until <code>leaseTime</code>
     * have passed since the lock was granted - whichever comes first.
     *
     * @param waitTime  the maximum time to aquire the lock
     * @param leaseTime lease time
     * @return <code>true</code> if lock has been successfully acquired
     * @throws InterruptedException - if the thread is interrupted before or during this method.
     */
    boolean tryLock(Duration waitTime, Duration leaseTime) throws InterruptedException;

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
     * Checks if this lock locked by any thread
     *
     * @return <code>true</code> if locked otherwise <code>false</code>
     */
    boolean isLocked();

    /**
     * Checks if this lock is held by the current thread
     *
     * @param threadId Thread ID of locking thread
     * @return <code>true</code> if held by given thread
     * otherwise <code>false</code>
     */
    boolean isHeldByThread(long threadId);

    /**
     * Checks if this lock is held by the current thread
     *
     * @return <code>true</code> if held by current thread
     * otherwise <code>false</code>
     */
    boolean isHeldByCurrentThread();

    /**
     * Number of holds on this lock by the current thread
     *
     * @return holds or <code>0</code> if this lock is not held by current thread
     */
    @Deprecated
    int getHoldCount();

    /**
     * Remaining time to live of this lock
     *
     * @return time in milliseconds
     * -2 if the lock does not exist.
     * -1 if the lock exists but has no associated expire.
     */
    Duration remainTimeToLive();

    /**
     * Returns name of object
     *
     * @return name - name of object
     */
    String getName();

}
