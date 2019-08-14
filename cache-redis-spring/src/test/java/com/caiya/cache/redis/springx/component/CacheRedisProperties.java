package com.caiya.cache.redis.springx.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cache.redis")
@Data
public class CacheRedisProperties {

    private String hostNames = "localhost:6379";

    private String password;

    private int maxRedirects = 10;

    @Deprecated
    private String masterName = "myMaster";

    private int maxTotal = -1;

    private int maxIdle = 100;

    private int minIdle = 10;

    private int maxWaitMillis = 1000;

    private String keySerializer;

    private String valueSerializer;

    private String hashKeySerializer;

    private String hashValueSerializer;

    private String defaultCacheName;

    private boolean useKeyPrefix = true;

    private String keyPrefix;

    private long defaultExpirationSeconds = 0;

}
