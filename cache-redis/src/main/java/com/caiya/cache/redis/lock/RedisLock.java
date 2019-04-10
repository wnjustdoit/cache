package com.caiya.cache.redis.lock;

import com.caiya.cache.CacheApi;
import com.caiya.cache.SetOption;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 使用redis实现分布式锁.
 * <p>
 * 简易版实现,具有以下特点:
 * 1、不可重入性。对于其它线程来说,具有互斥性,而对于本身来说,具有不可重入性;
 * 2、删除锁的保证性。删除锁最简单的是使用del命令,但是不能保证删除的是当前线程加的锁,这里用的是lua脚本实现,保证了原子操作;
 * 3、不可靠性。本实例是基于redis单节点(或单个master node)实现的,具有不可靠性;
 * 4、读写未分离。没有区分读写锁,所有读写操作一视同仁,效率方面在读多写少场景下可能会有一些明显损耗;
 * 5、锁等待时间。粗略将锁需要等待的时间切片,在指定的最大等待锁定时间内有固定的尝试次数和每次尝试未果阻塞等待的时间,鉴于锁的失效分为两种:手动释放和自动失效,
 * 所以可以对这一点考虑优化;
 * 6、死锁的防止。可重入性和锁的失效策略决定了死锁是否发生,这里只通过redis的自动失效时间保证了在最大失效时间之内锁一定会得到释放,随之而来的一个问题是:
 * 假如程序在锁失效时间内还没处理完程序,导致意外可能的并发,这种情形怎么处理?
 * <p>
 * redis锁的实现基于对redis的key进行锁定,同时value需要在解锁的时候进行校验.
 * <p>
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisLock extends AbstractLock<String> {

    private static final Unsafe UNSAFE;

    /**
     * 每两次尝试的最大间隔时间为{@link #MAX_TRY_DURATION_TIME}秒
     */
    private static final Duration MAX_TRY_DURATION_TIME = Duration.ofSeconds(2);

    /**
     * 由于本地时钟、redis服务端性能地下等原因造成每两次尝试的时间间隔异常的最大次数为{@link #MAX_TRY_ERROR_TIMES}次
     */
    private static final int MAX_TRY_ERROR_TIMES = 2;

    /**
     * 操作缓存的客户端或API
     */
    private CacheApi<String, String> cache;

    /**
     * 锁的value,需要保证全局唯一(包括在分布式环境中)
     */
    private final String uniqueGlobalValue;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unsafe initialize failed");
        }
    }

    public RedisLock(CacheApi<String, String> cache, String target) {
        if (cache == null) {
            throw new IllegalArgumentException("the based-on cache client cannot be null");
        }
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("the redis lock key cannot be null or empty");
        }

        this.cache = cache;
        setTarget(target);
        // 生成与当前线程关联的全局唯一id,用以标识锁的归属
        this.uniqueGlobalValue = getRandomUUID();
    }


    protected <R> R execute(LockCallBack<R> callBack, String cacheKeyPrefix) {
        if (callBack == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        if (cacheKeyPrefix == null || cacheKeyPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("cache key prefix cannot be null or empty");
        }

        boolean locked = lock();
        if (locked) {
            try {
                return callBack.onSuccess();
            } finally {
                unlock0(cacheKeyPrefix);
            }
        }

        callBack.onFailure(new RuntimeException("redis lock failed"));
        return null;
    }

    @Override
    public boolean lock() {
        return tryLock(getMaxWaitTime());
    }

    @Override
    public boolean tryLockOnce() {
        return tryLock(null);
    }

    @Override
    public boolean tryLock(Duration maxWaitTime) {
        if (maxWaitTime == null) {
            return lockInternal();
        }

        long start = System.nanoTime();
        int errorCount = 0;
        int index = 0;
        do {
//            if (logger.isDebugEnabled()) {
//                logger.debug("current thread:{}, attempt time:{}", Thread.currentThread().getName(), ++index);
//            }
            long eachStart = System.nanoTime();
            if (lockInternal()) {
                return true;
            }
            long eachDuration = System.nanoTime() - eachStart;
            if (eachDuration < 0 || eachDuration > MAX_TRY_DURATION_TIME.toNanos()) {
                // 重置休眠时间(正常情况下不会进入)
                if (errorCount > MAX_TRY_ERROR_TIMES) {
                    logger.warn("try lock:{}, exceeds {} MAX_TRY_ERROR_TIMES.", getTarget(), MAX_TRY_ERROR_TIMES);
                    return false;
                }
                eachDuration = Duration.ofSeconds(2).toNanos();
                start = System.nanoTime();
                errorCount++;
            }
            if ((System.nanoTime() - start) >= maxWaitTime.toNanos()) {
                return false;
            }
            // 强制阻塞等待(这里切成时间碎片,碎片时间为前一次请求redis服务端的时间,可以调整,后续进一步优化)
            UNSAFE.park(false, eachDuration);
        } while (true);
    }

    private boolean lockInternal() {
        return "OK".equals(cache.set(getTarget(), getLockValue(), SetOption.SET_IF_ABSENT, getLockTimeout().toMillis(), TimeUnit.MILLISECONDS));
    }

    @Override
    public void unlock() {
        cache.eval("if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",
                1, getTarget(), getLockValue());
    }

    /**
     * 兼容缓存前缀.
     *
     * @param keyPrefix the cache key prefix.
     */
    public void unlock0(String keyPrefix) {
        cache.eval("if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",
                1, keyPrefix + getTarget(), getLockValue());
    }

    /**
     * 如果是分布式环境,构成形式可以是:MacId + JvmRoute + ThreadId.
     * <p>
     * 这里暂且使用UUID标记.
     *
     * @return the unique global lock value
     */
    private String getLockValue() {
        return this.uniqueGlobalValue;
    }

    private String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

}
