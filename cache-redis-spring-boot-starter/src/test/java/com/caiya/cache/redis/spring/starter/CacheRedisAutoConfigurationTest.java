package com.caiya.cache.redis.spring.starter;

import com.caiya.cache.redis.RedisTemplate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CacheRedisApplication.class)
public class CacheRedisAutoConfigurationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void test() {
        String key = "测试key";
        String value = "测试value";
        redisTemplate.set(key, value, 60);
        Assert.assertEquals(value, redisTemplate.get(key));
    }

    @Test
    public void test_hash() {
        String key = "测试hash.key";
        String hkey = "测试hash.hkey";
        String hvalue = "测试hash.hvalue";
        redisTemplate.hSet(key, hkey, hvalue);
        redisTemplate.expire(key, 60);
        Assert.assertEquals(hvalue, redisTemplate.hGet(key, hkey));
    }

    @Test
    public void test_cacheManager() {
        Cache cache = cacheManager.getCache("caiya_cache");
        Assert.assertNotNull(cache);
        String key = "测试cacheManager";
        Object value = "测试cacheManager_value";
        cache.put(key, value);
        Assert.assertEquals(value, cache.get(key).get());
        cache.evict(key);
        Assert.assertNull(cache.get(key));
    }


}
