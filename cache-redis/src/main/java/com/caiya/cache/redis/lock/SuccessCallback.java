package com.caiya.cache.redis.lock;

/**
 * Success callback for lock.
 */
public interface SuccessCallback<T> {

    /**
     * Called when the lock completes with success.
     */
    T onSuccess();

}
