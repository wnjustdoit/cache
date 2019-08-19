package com.caiya.cache.redis.lock;

import com.caiya.cache.CacheApi;
import com.caiya.cache.redis.util.Constant;
import com.caiya.cache.redis.JedisCache;
import com.caiya.serialization.jdk.StringSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * RedisLockTest.
 *
 * @author wangnan
 * @since 1.0
 */
public class RedisLockTest {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockTest.class);

    private RedisLockFactory redisLockFactory;

    @Before
    public void before() {
        initLockFactory();
    }

    @After
    public void after() {
        redisLockFactory.destroy();
    }

    private void initLockFactory() {
        Set<HostAndPort> hostAndPorts = new HashSet<>();
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7000));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7001));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7002));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7003));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7004));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7005));
        JedisCluster jedisCluster = new JedisCluster(hostAndPorts);
        StringSerializer stringSerializer = new StringSerializer();
        CacheApi<String, String> cache = new JedisCache<>(jedisCluster);
        ((JedisCache) cache).setKeySerializer(stringSerializer);
        ((JedisCache) cache).setValueSerializer(stringSerializer);
        ((JedisCache) cache).setKeyPrefix((Constant.DEFAULT_CACHE_NAME + ":lock:").getBytes(StandardCharsets.UTF_8));
        redisLockFactory = RedisLockFactory.create(cache);
    }


    /**
     * 只尝试一次加锁
     */
    @Test
    public void testTryLockManual() {
        RedisLock redisLock = redisLockFactory.buildLock("Mutuki官方旗舰店");
        redisLock.setDefaultLeaseTime(Duration.ofSeconds(30));
        boolean locked = redisLock.tryLock();
        if (locked) {
            try {
                // do my business
                logger.info("do my business..");
            } finally {
                redisLock.unlock();
            }
        }
    }

    /**
     * 一直阻塞线程加锁
     * <p>
     * 实际尝试最大时间： {@link java.lang.Integer#MAX_VALUE} 秒进行加锁
     * </p>
     */
    @Test
    public void testLockManual() {
        RedisLock redisLock = redisLockFactory.buildLock("Mutuki官方旗舰店");
        redisLock.setDefaultWaitTime(Duration.ofSeconds(30));
        redisLock.setDefaultLeaseTime(Duration.ofSeconds(30));
        redisLock.lock();
        try {
            // do my business
            logger.info("do my business..");
        } finally {
            redisLock.unlock();
        }
    }


    @Test
    public void testSingle() {
        testTryOnceCallbackInternal("Mutuki官方旗舰店");
    }

    @Test
    @Ignore
    public void testParallel() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1000);
        for (int i = 0; i < 100000; i++) {
            executorService.execute(new Thread(() -> {
                testTryOnceCallbackInternal("Mutuki官方旗舰店");
            }));
            try {
                TimeUnit.SECONDS.sleep(new Random().nextInt(2));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
    }

    private void testTryOnceCallbackInternal(String lockKey) {
        RedisLock redisLock = redisLockFactory.buildLock(lockKey);
        redisLock.setDefaultLeaseTime(Duration.ofSeconds(30));
        logger.info("current lockKey:{}, lockName:{}", lockKey, redisLock.getName());
        String result = redisLock.execute(new LockCallBack<String>() {
            @Override
            public String onSuccess() {
                // do my business
                logger.info("lock success, current lockKey:{}, lockName:{}, isHeldByCurrentThread:{}, remainTimeToLive:{}", lockKey, redisLock.getName(), redisLock.isHeldByCurrentThread(), redisLock.remainTimeToLive());
                try {
                    TimeUnit.SECONDS.sleep(new Random().nextInt(2));
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
                return Thread.currentThread().getName();
            }

            @Override
            public void onFailure(Throwable throwable) {
                // may handle exception
                logger.error(throwable.getMessage(), throwable);
            }
        });
        if (result != null) {
            logger.info("lock success, result is: " + result);
        } else {
            logger.info("lock failed, current thread is: " + Thread.currentThread().getName());
        }
    }

}
