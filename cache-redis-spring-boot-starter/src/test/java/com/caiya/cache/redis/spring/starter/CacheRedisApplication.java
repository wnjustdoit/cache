package com.caiya.cache.redis.spring.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.caiya")
public class CacheRedisApplication {


    public static void main(String[] args) {
        SpringApplication.run(CacheRedisApplication.class);
    }

}
