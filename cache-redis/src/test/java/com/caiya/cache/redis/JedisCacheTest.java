package com.caiya.cache.redis;

import com.caiya.serialization.jdk.JdkSerializationSerializer;
import com.caiya.serialization.jdk.StringSerializer;
import org.junit.*;
import org.junit.runners.MethodSorters;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.io.IOException;
import java.util.*;

/**
 * JedisCacheTest.
 *
 * @author wangnan
 * @since 1.1
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JedisCacheTest extends BaseCacheTest {

    @Before
    public void setUp() {
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
        ((JedisCache) cache).setValueSerializer(jdkSerializationSerializer);
        ((JedisCache) cache).setHashKeySerializer(stringSerializer);
        ((JedisCache) cache).setHashValueSerializer(jdkSerializationSerializer);
//        ((JedisCache) cache).setKeyPrefix((Constant.DEFAULT_CACHE_NAME + ":").getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void tearDown() {
        try {
            ((JedisCache) cache).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    protected long getLength(Object obj) {
        return (((JedisCache) cache).getValueSerializer()).serialize(obj).length;
    }

}