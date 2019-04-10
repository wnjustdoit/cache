#### 缓存模块
* 基于rediscluster(redis版本3.+以上);
* 客户端使用(Socket、)Jedis、JedisCluster以及spring-data-redis;
* cache-redis模块使用JedisCluster实现,cache-redis-integration-spring使用spring-data-redis实现(集成spring框架,并支持更高级的cache注解);
* 需要注意的是:JedisCluster底层实现默认使用连接池,并对多key的命令支持不全,因为多个key不一定在同一个hash槽或者节点上

##### 特殊实现的命令或功能如下:
* rename(K oldKey, K newKey);
* del(K... keys);
* ...