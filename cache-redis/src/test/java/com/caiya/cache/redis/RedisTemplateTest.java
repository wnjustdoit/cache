package com.caiya.cache.redis;

import com.caiya.serialization.Serializer;
import com.caiya.serialization.jdk.JdkSerializationSerializer;
import com.caiya.serialization.jdk.StringSerializer;
import org.junit.*;
import org.junit.runners.MethodSorters;
import redis.clients.jedis.HostAndPort;

import java.util.*;

/**
 * RedisTemplateTest.
 *
 * @author wangnan
 * @since 1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RedisTemplateTest extends BaseCacheTest {

    private RedisTemplate<String, Object> redisTemplate;

    @Before
    public void setUp() {
        Set<HostAndPort> hostAndPorts = new HashSet<>();
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7000));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7001));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7002));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7003));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7004));
        hostAndPorts.add(new HostAndPort("192.168.1.249", 7005));
        StringSerializer stringSerializer = new StringSerializer();
        JdkSerializationSerializer jdkSerializationSerializer = new JdkSerializationSerializer();
        redisTemplate = new RedisTemplate<>();
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(hostAndPorts);
//        connectionFactory.setKeyPrefix((Constant.DEFAULT_CACHE_NAME + ":").getBytes("UTF-8"));
        connectionFactory.setKeySerializer(stringSerializer);
        connectionFactory.setValueSerializer(jdkSerializationSerializer);
        connectionFactory.setHashKeySerializer(stringSerializer);
        connectionFactory.setHashValueSerializer(jdkSerializationSerializer);
        redisTemplate.setConnectionFactory(connectionFactory);
        cache = redisTemplate;
    }

    @After
    public void tearDown() {
        redisTemplate.getConnectionFactory().destroy();
    }


    @SuppressWarnings("unchecked")
    @Override
    protected long getLength(Object obj) {
        return ((Serializer<Object>) ((JedisConnectionFactory) ((RedisTemplate<String, Object>) cache).getConnectionFactory()).getValueSerializer()).serialize(obj).length;
    }

}