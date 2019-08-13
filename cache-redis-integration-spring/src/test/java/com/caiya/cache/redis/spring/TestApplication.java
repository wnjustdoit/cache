package com.caiya.cache.redis.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application
 *
 * @author wangnan
 * @since 1.0
 */
@SpringBootApplication(scanBasePackages = "com.caiya")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

}
