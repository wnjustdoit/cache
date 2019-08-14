package com.caiya.cache.redis.springx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.caiya")
public class CacheManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheManagerApplication.class, args);
    }

}
