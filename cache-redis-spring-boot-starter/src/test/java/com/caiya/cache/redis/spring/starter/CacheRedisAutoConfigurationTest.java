package com.caiya.cache.redis.spring.starter;

import com.caiya.cache.redis.RedisTemplate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CacheRedisApplication.class)
public class CacheRedisAutoConfigurationTest {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisTemplate<String, String> stringRedisTemplate;

    @Resource
    private CacheManager cacheManager;

    @Test
    public void test() {
        String key = "测试key";
        String key2 = "测试key2";
        String value = "测试value";

        redisTemplate.set(key, value, 60);
        Assert.assertEquals(value, redisTemplate.get(key));

        stringRedisTemplate.set(key2, value, 60);
        Assert.assertEquals(value, stringRedisTemplate.get(key2));
    }

    @Test
    public void test_hash() {
        String key = "测试hash.key";
        String key2 = "测试hash.key2";
        String hkey = "测试hash.hkey";
        String hvalue = "测试hash.hvalue";

        redisTemplate.hSet(key, hkey, hvalue);
        redisTemplate.expire(key, 60);
        Assert.assertEquals(hvalue, redisTemplate.hGet(key, hkey));

        stringRedisTemplate.hSet(key2, hkey, hvalue);
        stringRedisTemplate.expire(key2, 60);
        Assert.assertEquals(hvalue, stringRedisTemplate.hGet(key2, hkey));
    }

    @Test
    public void test_cacheManager() {
        Cache cache = cacheManager.getCache("caiya_cache");
        Assert.assertNotNull(cache);
        String key = "测试cacheManager";
        Object value = "测试cacheManager_value";
        cache.put(key, value);
        Cache.ValueWrapper valueWrapper = cache.get(key);
        Assert.assertNotNull(valueWrapper);
        Assert.assertEquals(value, valueWrapper.get());
        cache.evict(key);
        Assert.assertNull(cache.get(key));
    }


}
