package com.caiya.cache.redis.lock;

/**
 * Lock CallBack Interface.
 *
 * @author wangnan
 * @since 1.0
 */
public interface LockCallBack<T> extends SuccessCallback<T>, FailureCallback {


}
