package com.caiya.cache.redis.lock;

/**
 * Failure callback for a lock.
 */
public interface FailureCallback {

    /**
     * Called when the lock completes with failure.
     */
    void onFailure(Throwable throwable);

}
