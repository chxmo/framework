# xuanmo中间件框架之redis spring boot starter

采用lettuce为redis客户端，提供redis功能的spring boot快速启动jar，开箱即用！

功能点：
- RedisUtils Redis缓存工具类
    - 该工具类主要包含常用的缓存操作,事物和pipleline相关建议直接用RedisUtils.execute()
- RedisLockUtils Redis分布式锁工具类
    - 采用lua脚本加锁和解锁
    - 支持可重入锁
    - 解锁增加是否当前线程加锁验证
    - 支持同一个线程持有多把锁
- @RedisLock注解支持，key名生成模式如下：
    - 自动模式，支持自动生成key名，格式：类型:参数值
    - 手动模式，利用SpringEl表达式支持动态参数生成类名
- 默认支持Redis健康检查机制（需要引入spring-boot-starter-actuator即可）
    
## 注意事项  
    1. **加锁操作**加锁操作一定要用返回值来判断是否加锁成功,如存在锁竞争情况，目前采用的是自旋机制，最多每次自旋10次，每次500ms，也就是加锁超时默认时间为5s  
    2. **解锁操作**如果同一个线程对同一个key重复加锁多次（每次加锁都会重新设置过期时间），解锁也需要对该key重复解锁相应的次数，最后一次才会解锁成功。  
    3. 如果redis服务是用云服务的话，请确保云服务的redis版本大于3.2（redis分布式锁是采用lua脚本，低版本不支持）.  
    4. 考虑到服务号拆分，redis服务器公用问题，一个大应用包含多个小应用(服务)，每个项目都要配置项目的缓存前缀来隔离缓存key重复问题，  
       每个业务团队在使用缓存前都需要确定自己的前缀是唯一的。
    5. 如果业务系统需要在初始化bean(构造函数/@PostConstruct/InitializingBean接口/@Bean(initMethod="***"))时使用Redis工具类，请在bean类前加入@DependsOn({"redisApplicationInit"})即可. 
## 如何开始
1.引入spring-boot-starter-lettuce-redis包

```xml
<dependency>
  <groupId>com.xm.boot</groupId>
  <artifactId>spring-boot-starter-lettuce-redis</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2.配置方式

配置方式提供3中模式，该配置xm.redis.conf.enabled=true必须要开启状态。

- Redis单节点模式/云服务redis
```yaml
xm:  
  redis:
    conf:
      enabled: true #是否开启-必填
      prefix: appName:serverName # 项目前缀列子(推荐用 应用名:服务名，注意这个前缀不要太长)-必填
      host: localhost #节点地址-必填
      port: 6379 #节点端口-必填
      ssl: false #是否开启ssl链接，默认false
      password: ****** #密码，默认无 
      database: 0 #数据库索引，默认是0
      timeout: 1000 #连接超时时间，模式1000ms
      share-native-connection: true #共享连接配置，默认为true
      pool:
        max-active: 10 #连接池最大连接数，默认是10（使用负值表示没有限制）
        max-wait: -1 #连接池最大阻塞等待时间,默认-1（使用负值表示没有限制）
        max-idle: 3 #连接池中的最大空闲连接，默认是3
        min-idle: 0 #连接池中的最小空闲连接，默认是0
```

- Redis哨兵模式
```yaml
xm:
  redis:
    conf:
      enabled: true #是否开启-必填
      prefix: appName:serverName # 项目前缀列子(推荐用 应用名:服务名，注意这个前缀不要太长)-必填
      ssl: false #是否开启ssl链接，默认false
      password: ****** #密码，默认无
      database: 0 #数据库索引，默认是0
      timeout: 1000 #连接超时时间，模式1000ms
      share-native-connection: true #共享连接配置，默认为true
      pool:
        max-active: 10 #连接池最大连接数，默认是10（使用负值表示没有限制）
        max-wait: -1 #连接池最大阻塞等待时间,默认-1（使用负值表示没有限制）
        max-idle: 3 #连接池中的最大空闲连接，默认是3
        min-idle: 0 #连接池中的最小空闲连接，默认是0
      sentinel:
        master: myster #哨兵master名称
        nodes: 192.168.1.1,192.168.2.1 #哨兵节点
        port: 6379 # 端口号，考虑到pk注入，需要将端口号提取出来
```

- Redis集群模式
```yaml
xm:
  redis:
    conf:
      enabled: true #是否开启-必填
      prefix: appName:serverName # 项目前缀列子(推荐用 应用名:服务名，注意这个前缀不要太长)-必填
      ssl: false #是否开启ssl链接，默认false
      password: ****** #密码，默认无
      database: 0 #数据库索引，默认是0
      timeout: 1000 #连接超时时间，模式1000ms
      share-native-connection: true #共享连接配置，默认为true
      pool:
        max-active: 10 #连接池最大连接数，默认是10（使用负值表示没有限制）
        max-wait: -1 #连接池最大阻塞等待时间,默认-1（使用负值表示没有限制）
        max-idle: 3 #连接池中的最大空闲连接，默认是3
        min-idle: 0 #连接池中的最小空闲连接，默认是0
      cluster:
        nodes: 192.168.1.1:6379,192.168.2.1:6379 #集群节点
        maxRedirects: 3 #重定向最大数
```

3.使用方法
``` java
  //如果需要指定redis缓存前缀操作，请使用以下代码，该代码适用于与RedisUtils工具类同步的情况下才能生效，仅支持一个工具类操作
  RedisContext.setPrefix("appname:servername");
  
  //缓存操作类,更多方法见RedisUtils.java
  RedisUtils.put("a","a");
  RedisUtils.get("a",String.class);

  //list 阻塞式获取方法需要注意超时时间的设置一定要小于redis设置的超时时间
  RedisUtils.blpop("aaa",5,String.class);
  RedisUtils.brpop("aaa",5,String.class);

  //原子操作，切记num字段不能为小数
  RedisUtils.incrByFloat("incr",1);

  //Redis分布式锁工具类,res为true表示成功获取锁
  //默认获取锁超时时间为5m
  //获取锁成功后，默认锁持有时间为60s
  boolean res = RedisLockUtils.lock("lockTest");//默认锁持有时间为60s，默认获取锁超时时间为5s
  //OR
  boolean res = RedisLockUtils.lock("lockTest",30);//锁持有时间为30秒
  try{
    //do something....
  }finally{
    //最好每次手动释放锁
    RedisLockUtils.unlock("lockTest");
  }
  
  //或者可以直接使用RedisTemplate，直接注入即可
  @Autowired
  private RedisTemplate redisTemplate;
  
```

4.@RedisLock注解详解

**参数含义：**

- cacheName: 用于表示最小颗粒度的缓存前缀，默认为空，非必填
- keys: 用于redis的key名，分为自动模式和动态参数模式，默认为自动模式，非必填
    > 自动模式：如果默认是{},则采用自动模式，用方法和参数来生成key名,生成的key名为  cacheName:方法名:参数值，其中参数值有如下几种情况：
    > 1. 如果方法没有参数，则使用 NO_PARAM;
    > 2. 如果方法参数中存在NULL，则使用 NULL_PARAM;
    > 3. 如果方法参数中存在包装类，则使用 包装类的值；
    > 4. 如果反复参数中存在对象，并且对象不为空，则使用 hc_+对象的hashcode;

    > 动态参数模式：如果不为空，则采用动态参数模式，通过SpringEL表达式动态获取参数的值，最终组装生成key
    > 1. 如果参数为单个动态参数,例如参数如果是对象@RedisLock(keys ="'orderNo:'+#order.orderNo")，如果参数是基本类型@RedisLock(keys = "'orderNo:'+#orderNo")，则表示从方法的参数中动态获取值,生成的key名为cacheName:orderNo(orderNo的参数值) .
    > 2. 如果参数为多个动态参数，例如@RedisLock(keys = {"'userId:'+#userId","'orderNo:'+#orderNo"})，则表示从方法参数中动态取值，生成的key名为：cacheName:userId(userId的参数值):orderNo(orderNo的参数值).
- expire: 用于表示获取锁成功之后，设置key的过期时间，单位为 秒，默认60秒，非必填
- timeout: 用于表示获取锁的超时时间，单位为 秒，默认，默认30秒，非必填


**案例代码：**

```java
    /**
     * 一、自动生成key name规则： 类名+方法名+NO_PARAM
     * 1. 无参数
     * example key name:   order:TestService.defauleTest1:NO_PARAM
     */
    @RedisLock(cacheName = "order")
    public void defauleTest1(){
        LOGGER.info(".......");
    }

    /**
     * 一、自动生成key name规则： 类名+方法名+参数值
     * example key name:  order:TestService.defauleTest2:123456754523:1001
     * @param orderNo
     * @param userId
     */
    @RedisLock(cacheName = "order")
    public void defauleTest2(String orderNo, String userId){
        LOGGER.info(".......");
    }

    /**
     * 一、自动生成key name规则： 类名+方法名+NULL_PARAM
     * example userDTO is null,key name:  order:TestService.defauleTest3:NULL_PARAM
     * @param userDTO
     */
    @RedisLock(cacheName = "order")
    public void defauleTest3(UserDTO userDTO){
        LOGGER.info(".......");
    }

    /**
     * 一、自动生成key name规则： 类名+方法名+hc_hashcode
     * example userDTO is not null,key name:  order:TestService.defauleTest4:hc_82590610
     * @param userDTO
     */
    @RedisLock(cacheName = "order")
    public void defauleTest4(UserDTO userDTO){
        LOGGER.info(".......");
    }

    /**
     * 二、动态获取参数-获取基本类型值
     * exmaple key name：  order:userId:123456754523:orderNo:1001
     * @param orderNo
     * @param userId
     */
    @RedisLock(cacheName = "order",keys ={"'userId:'+#userId","'orderNo:'+#orderNo"})
    public void domainTest1(String orderNo, String userId){
        LOGGER.info(".......");
    }

    /**
     * 二、动态获取参数-获取对象的值
     * exmaple key name：  order:userId:123456754523:orderNo:1001
     * @param userDTO
     */
    @RedisLock(cacheName = "order",keys ={"'userId:'+#userDTO.userId","'orderNo:'+#userDTO.orderNo"})
    public void domainTest2(UserDTO userDTO){
        LOGGER.info(".......");
    }
```