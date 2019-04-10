package com.caiya.cache.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 单元测试基类
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BaseTest.AutoConfig.class, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Test
    public void test() throws Exception {
        System.out.println("========");
    }

    @ComponentScan(basePackages = "com.caiya")
    @EnableAutoConfiguration
    public static class AutoConfig {

    }


}
