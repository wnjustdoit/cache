package com.caiya.cache.redis.spring;

import com.caiya.cache.CacheApi;
import com.caiya.cache.RedisConstant;
import com.caiya.cache.SetOption;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import redis.clients.util.JedisClusterCRC16;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * RedisCacheTest.
 *
 * @author wangnan
 * @since 1.1
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RedisCacheTest {

    private CacheApi<String, Object> cache;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Before
    public void setUp() {
        cache = new RedisCache<>(redisTemplate);
    }

    @After
    public void tearDown() {

    }


    @Test
    public void test_Z1_Del() {
        long result = cache.del("test_jedis_cache");
        assertEquals(1, result);
    }

    @Test
    public void test_Z2_Del() {
        cache.set("test_jedis_cachex", "", 1000);
        cache.set("test_jedis_cachey", "", 1000);
        long result = cache.del("test_jedis_cachex", "test_jedis_cachey");
        assertEquals(2, result);
        assertFalse(cache.exists("test_jedis_cachex"));
        assertFalse(cache.exists("test_jedis_cachey"));
    }

    @Test
    public void test_A1_Set() {
        cache.set("test_jedis_cache", "测试jedis缓存", 60 * 60);
    }

    @Test
    public void test_A2_Set() {
        cache.set("test_jedis_cache2", "测试jedis缓存2", 60 * 60, TimeUnit.SECONDS);
    }

    @Test
    public void test_A3_Set() {
        cache.del("test_jedis_cache3");// init

        String result = cache.set("test_jedis_cache3", "测试jedis缓存2", SetOption.ifPresent(), 60 * 60, TimeUnit.SECONDS);
        result = cache.set("test_jedis_cache3", "测试jedis缓存2", SetOption.ifAbsent(), 60 * 60, TimeUnit.SECONDS);
        result = cache.set("test_jedis_cache3", "测试jedis缓存2", SetOption.ifPresent(), 60 * 60, TimeUnit.SECONDS);
        result = cache.set("test_jedis_cache3", "测试jedis缓存2", SetOption.ifAbsent(), 60 * 60, TimeUnit.SECONDS);

        cache.del("test_jedis_cache3");// delete
    }

    @Test
    public void test_B_Get() {
        Object result = cache.get("test_jedis_cache");
        assertEquals("测试jedis缓存", result);
    }

    @Test
    @Ignore
    public void testKeys() {
        cache.keys("");
    }

    @Test
    @Ignore
    public void test_C_Scan() {
        List<String> keySet = cache.scan("erp_session*", 100);
        assertTrue(keySet.size() > 0);
    }

    @Test
    @Ignore
    public void testScan1() {
        cache.scan("", "", 1);
    }

    @Test
    public void test_D_Exists() {
        boolean result = cache.exists("test_jedis_cache");
        assertTrue(result);
    }

    @Test
    public void test_E_Expire() {
        boolean result = cache.expire("test_jedis_cache", 60 * 60);
        assertTrue(result);
    }

    @Test
    public void test_F1_Ttl() {
        long result = cache.ttl("test_jedis_cache");
        assertTrue(result > 0);
    }

    @Test
    public void test_F_A_Rename() {
        String sourceKey, targetKey;
        // 相同hash槽
        sourceKey = "test3504";
        targetKey = "test6276";
        assertEquals(JedisClusterCRC16.getSlot(sourceKey), JedisClusterCRC16.getSlot(targetKey));
        testRename(sourceKey, targetKey, true, null);
        testRename(sourceKey, targetKey, true, RedisConstant.Operation.HASH);

        // 不同hash槽
        sourceKey = "test1234";
        targetKey = "test2345";
        assertNotEquals(JedisClusterCRC16.getSlot(sourceKey), JedisClusterCRC16.getSlot(targetKey));
        testRename(sourceKey, targetKey, false, null);
        testRename(sourceKey, targetKey, false, RedisConstant.Operation.HASH);
    }

    private void testRename(String sourceKey, String targetKey, boolean isSameSlot, RedisConstant.Operation operation) {
        cache.del(sourceKey);
        cache.del(targetKey);
        if (operation == RedisConstant.Operation.HASH) {
            Map<String, Object> hashes = new HashMap<>();
            hashes.put("test", "");
            cache.hMSet(sourceKey, hashes);
            cache.expire(sourceKey, 1000);
        } else {
            cache.set(sourceKey, "", 1000);
        }
        assertTrue(cache.exists(sourceKey));
        assertFalse(cache.exists(targetKey));
        cache.rename(sourceKey, targetKey, operation);
        assertFalse(cache.exists(sourceKey));
        assertTrue(cache.exists(targetKey));
        // spring-data-redis bug!
        if (isSameSlot) {
            assertTrue(cache.ttl(targetKey) > 900 && cache.ttl(targetKey) <= 1000);
        } else {
            assertEquals(cache.ttl(targetKey), -1);
        }
        // spring-data-redis bug!
        assertTrue(cache.del(targetKey) > 0);
        assertFalse(cache.exists(targetKey));
    }

    @Test
    public void test_F_B_Append() {
        Object value = cache.get("test_jedis_cache");
        String appendVal = "append我";
        Long result = cache.append("test_jedis_cache", appendVal);
        assertEquals(getLength(value) + getLength(appendVal), (long) result);
    }

    private long getLength(Object obj) {
        return (((RedisCache) cache).getValueSerializer()).serialize(obj).length;
    }

    @Test
    @Ignore
    public void testFlushDB() {
        cache.flushDB();
    }

    @Test
    public void testDbSize() {
        cache.set("testdbsize", "", 600);
        assertTrue(cache.dbSize() > 1);
        cache.del("testdbsize");
    }

    @Test
    public void testPing() {
        assertEquals("PONG", cache.ping());
    }

    @Test
    public void test_G1_HSet() {
        boolean result = cache.hSet("测试hash", "姓名", "张三");
        assertTrue(result);
    }

    @Test
    public void test_G2_HGet() {
        Object result = cache.hGet("测试hash", "姓名");
        assertEquals("张三", result);
    }

    @Test
    public void test_G3_HExists() {
        Boolean result = cache.hExists("测试hash", "姓名");
        assertTrue(result);
    }

    @Test
    public void test_G4_HKeys() {
        Set<String> keySet = cache.hKeys("测试hash");
        assertTrue(keySet.size() > 0);
    }

    @Test
    public void test_G9_HDel() {
        long result = cache.hDel("测试hash", "姓名");
        assertEquals(1L, result);
    }

    @Test
    public void test_G5_HMSet() {
        Map<String, Object> hashes = new HashMap<>();
        hashes.put("年龄", 3);
        hashes.put("性别", "男");
        cache.hMSet("测试hash", hashes);
    }

    @Test
    public void test_G6_HGetAll() {
        Map<String, Object> result = cache.hGetAll("测试hash");
        assertNotNull(result);
        assertTrue(result.size() > 2);
        assertEquals(3, result.get("年龄"));
        assertEquals("男", result.get("性别"));
    }

    @Test
    public void test_H1_LPush() {
        long result = cache.lPush("测试list", "123", "abc", "就是我");
        assertTrue(result >= 3);
    }

    @Test
    public void test_H2_LRange() {
        List<Object> result = cache.lRange("测试list", 0, 1);
        assertTrue(result.size() > 0);
    }

    @Test
    public void test_H3_LRem() {
        long result = cache.lRem("测试list", 0, "abc");
        assertTrue(result > 0);
    }

    @Test
    public void test_H4_LTrim() {
        cache.lTrim("测试list", 0, 1);
    }

    @Test
    public void test_H5_LPop() {
        Object result = cache.lPop("测试list");
        assertNotNull(result);
    }

    @Test
    public void test_H6_LLen() {
        long result = cache.lLen("测试list");
        assertTrue(result > 0);
    }

    @Test
    @Ignore
    public void test_I_eval() {
        Object result = cache.eval("if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end", 1, "test_jedis_cache", "vvvvalue");
        assertEquals(result, 0L);
    }


}