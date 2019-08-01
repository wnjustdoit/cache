package com.caiya.cache.redis.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

/**
 * Abstract class for Lock Implementations.
 *
 * @author wangnan
 * @since 1.0
 */
public abstract class AbstractLock<T> implements Lock<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final long DEFAULT_LOCK_EXPIRATION_SECONDS = 20;

    private static final long DEFAULT_TRY_LOCK_DURATION_SECONDS = Integer.MAX_VALUE;

    /**
     * The lock name.
     */
    private String name;

    /**
     * The unique lock value in distributed situation, associated with current thread
     */
    private String value;

    /**
     * The lock timeout. To avoid dead lock.
     */
    private Duration lockTimeout = Duration.ofSeconds(DEFAULT_LOCK_EXPIRATION_SECONDS);

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

        // here, try to lock only once!!
        boolean locked = tryLock();
        if (locked) {
            try {
                return callBack.onSuccess();
            } finally {
                unlock();
            }
        }

        callBack.onFailure(new RuntimeException("lock failed"));
        return null;
    }


    protected void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 如果是分布式环境，构成形式可以是:MacId + JvmRoute + ThreadId.
     * <p>
     * 默认使用UUID标记.
     * </p>
     *
     * @return the unique global lock value
     */
    protected String getValue() {
        if (this.value == null) {
            this.value = getRandomUUID();
        }
        return this.value;
    }

    public void setValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("lock value cannot be null");
        }
        this.value = value;
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    public void setLockTimeout(Duration lockTimeout) {
        if (lockTimeout == null) {
            throw new IllegalArgumentException("lock timeout cannot be null");
        }
        this.lockTimeout = lockTimeout;
    }

    @Override
    public Duration getLockTimeout() {
        return lockTimeout;
    }

}
