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
public abstract class AbstractLock<T> implements RLock<T> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Duration DEFAULT_MAX_LOCK_WAIT_TIME = Duration.ofSeconds(Integer.MAX_VALUE);

    private static final Duration DEFAULT_MAX_LOCK_LEASE_TIME = Duration.ofSeconds(Integer.MAX_VALUE);

    /**
     * The lock name
     */
    private String name;

    /**
     * The unique lock value in distributed situation, associated with current thread
     * <p>
     * If null at runtime, it will be a random UUID
     * </p>
     */
    private String valuePrefix;

    /*
    The value splitter, concat valuePrefix and threadId as the default
     */
    private String valueSplitter = "-";

    /**
     * Default lock wait time
     */
    private Duration defaultWaitTime = DEFAULT_MAX_LOCK_WAIT_TIME;

    /**
     * Default lock lease time
     */
    private Duration defaultLeaseTime = DEFAULT_MAX_LOCK_LEASE_TIME;

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
     * In distributed environment, it usually consists of: MacId + JvmRoute + ThreadId.
     * <p>
     * By default, use a random UUID as prefix
     * </p>
     *
     * @return the unique global lock value
     */
    String getValueByThreadId(long threadId) {
        if (this.valuePrefix == null) {
            this.valuePrefix = getRandomUUID().replaceAll("-", "");
        }
        return this.valuePrefix + this.valueSplitter + threadId;
    }

    public void setValuePrefix(String valuePrefix) {
        if (valuePrefix == null) {
            throw new IllegalArgumentException("lock valuePrefix cannot be null");
        }
        this.valuePrefix = valuePrefix;
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Set the default lock wait time
     *
     * @param defaultWaitTime default lock wait time
     */
    public void setDefaultWaitTime(Duration defaultWaitTime) {
        this.defaultWaitTime = defaultWaitTime;
    }

    public Duration getDefaultWaitTime() {
        return defaultWaitTime;
    }

    /**
     * set the default lock lease time
     *
     * @param defaultLeaseTime default lock lease time
     */
    public void setDefaultLeaseTime(Duration defaultLeaseTime) {
        this.defaultLeaseTime = defaultLeaseTime;
    }

    public Duration getDefaultLeaseTime() {
        return defaultLeaseTime;
    }
}
