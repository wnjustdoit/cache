## 基于Redis实现的缓存模块
---
#### 实现说明
* 服务端基于Redis Cluster（redis版本3.+以上）实现；
* 客户端使用（Socket、）Jedis、JedisCluster以及spring-data-redis；
* cache-redis模块使用JedisCluster实现；
* cache-redis-spring模块是在cache-redis基础之上，实现spring对cache的抽象；
* cache-redis-spring-boot-starter是基于cache-redis-spring（或cache-redis）模块自定义的starter；
* cache-redis-integration-spring使用spring-data-redis实现（后续准备废弃）

---
#### 特殊实现的命令
JedisCluster底层实现默认使用连接池，并对一些操作无法支持（比如：对多key操作的命令支持不全，因为多个key不一定在同一个hash槽或者节点上）。
* rename(K oldKey, K newKey)；
* del(K... keys)；
* ...

---
#### 用法
1. 在执行sh build.sh之前，先将自建的maven私服配置添加到各个模块的pom文件中：
```xml
    <distributionManagement>
        <repository>
            <id>releases</id>
            <name>local release repository</name>
            <url>http://your_nexus_domain/repository/maven-releases/</url>
        </repository>
        <snapshotRepository>
            <id>snapshots</id>
            <name>local snapshot repository</name>
            <url>http://your_nexus_domain/repository/maven-snapshots/</url>
        </snapshotRepository>
    </distributionManagement>
```
2. 执行sh build.sh命令，将jar包上传到maven私服；
3. 引入maven坐标到你的项目中：
```xml
    <!-- java环境依赖此项 -->
    <dependency>
        <groupId>com.caiya</groupId>
        <artifactId>cache-redis</artifactId>
        <version>${latest-version}</version>
    </dependency>
    
    <!-- spring环境依赖此项 -->
    <dependency>
        <groupId>com.caiya</groupId>
        <artifactId>cache-redis-integration-spring</artifactId>
        <version>${latest-version}</version>
    </dependency>
```
4. 具体用法见各模块的单元测试；基于注解的缓存用法见com.caiya.cache.redis.spring.ExtendedRedisCacheManager/com.caiya.cache.redis.springx.JedisCacheManager类前注释

---
#### 分布式锁
com.caiya.cache.redis.lock.RedisLockTest
```text
实现简单的互斥锁，暂不支持可重入性、读写锁等特性。
```
