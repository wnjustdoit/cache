package com.caiya.cache.redis.springx;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CacheManagerApplication.class)
public class CacheManagerTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void test() {
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
