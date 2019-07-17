package com.caiya.cache.test.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis集群的属性配置.
 *
 * @author wangnan
 * @since 1.0
 */
@Component
@ConfigurationProperties(prefix = "cache.redis")
@Data
public class RedisProperties {

    private int maxTotal = -1;

    private int maxIdle = 100;

    private int minIdle = 10;

    private int maxWaitMillis = 1000;

    private String hostNames = "localhost:6379";

    private String password;

    private int maxRedirects = 10;

    private String masterName = "myMaster";


}
