package com.caiya.cache.redis.lock;

import com.caiya.cache.redis.util.Constant;
import com.caiya.cache.Cache;
import com.caiya.cache.redis.JedisCache;
import com.caiya.serialization.jdk.JdkSerializationSerializer;
import com.caiya.serialization.jdk.StringSerializer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.nio.charset.StandardCharsets;
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

    private RedisLock redisLock;

    private Cache<String, String> cache;

    @Before
    public void before() {
        initJedisCluster();
        redisLock = new RedisLock(cache, "Mutuki官方旗舰店");
    }

    private void initJedisCluster() {
        Set<HostAndPort> hostAndPorts = new HashSet<>();
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7000));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7001));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7002));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7003));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7004));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7005));
        JedisCluster jedisCluster = new JedisCluster(hostAndPorts);
        StringSerializer stringSerializer = new StringSerializer();
        JdkSerializationSerializer jdkSerializationSerializer = new JdkSerializationSerializer();
        cache = new JedisCache<>(jedisCluster);
        ((JedisCache) cache).setKeySerializer(stringSerializer);
        ((JedisCache) cache).setValueSerializer(stringSerializer);
        ((JedisCache) cache).setKeyPrefix((Constant.DEFAULT_CACHE_NAME + ":lock:").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testSingle() {
        testInternal(redisLock);
    }

    private void testInternal(final RedisLock redisLock) {
        String result = redisLock.execute(new LockCallBack<String>() {
            @Override
            public String onSuccess() {
                // do my business
                // ...
                System.out.println("lock success, do my business, current thread is: " + Thread.currentThread().getName());
                try {
                    TimeUnit.SECONDS.sleep(new Random().nextInt(2));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return Thread.currentThread().getName();
            }

            @Override
            public void onFailure(Throwable throwable) {
                // may handle exception
//                System.out.println(throwable);
            }
        }, new String(((JedisCache) cache).getKeyPrefix()));
        if (result != null) {
            System.out.println("lock success, result is: " + result);
        } else {
            System.out.println("lock failed, current thread is: " + Thread.currentThread().getName());
        }
    }

    @Test
    @Ignore
    public void testParallel() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1000);
        for (int i = 0; i < 100000; i++) {
            executorService.execute(new Thread(() -> {
                RedisLock redisLock = new RedisLock(cache, "Mutuki官方旗舰店");
                testInternal(redisLock);
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


    @Test
    @Ignore
    public void testLock() {
        redisLock.lock();
        redisLock.unlock0(new String(((JedisCache) cache).getKeyPrefix()));
    }


}
