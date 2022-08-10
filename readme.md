# 使用数据库实现缓存供能

## 编写目的

在某些特殊的项目中，想实现缓存，但是不能使用中间件，存内存又会导致内存大幅度上升，怎么办呢？

降低预期，将需要缓存的数据存储在数据库，如何设计一套数据库缓存呢。

## 设计思路

一个KV形式缓存中间件需要有哪些基础功能？

* 1、增加缓存（新增数据库）
* 2、缓存覆盖（修改数据库）
* 3、缓存过期删除（删除数据库数据）
* 4、查询缓存（查询数据库）

其实，就是对数据库的增删改查。但是缓存的数据一般情况是写入和查询比较频繁的。

* 查询优化: 在字段KEY上建立唯一索引
* 插入优化：使用队列 + 定时任务异步入库
* 缓存覆盖：使用队列 + 定时任务异步更新
* 过期删除：使用队列 + 定时任务异步删除


## 表设计

主键，缓存KEY（唯一索引）, 缓存值 , 过期时间

```sql
create table data_common_cache
(
    cache_id        bigint auto_increment comment '主键，生成序列号Id' primary key,
    cache_key       varchar(100)   not null comment '缓存的key 长度100 超过100的话通过编码后缩短',
    cache_value     text  default null comment '缓存的值',
    cache_expire    datetime default null comment '过期时间',
    constraint udx_cache_key unique (cache_key)
) comment '通用缓存表';
```

## 功能实现

使用SpringBoot + Mybatis Plus 实现通用缓存功能

### 依赖引入
```sql
<properties>
    <java.version>1.8</java.version>
    <mybatis-plus.version>3.4.0</mybatis-plus.version>
    <mybatis-plus-generator.version>3.3.2</mybatis-plus-generator.version>
</properties>
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!--mybatis-plus-->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-generator</artifactId>
        <version>${mybatis-plus-generator.version}</version>
    </dependency>
    <!--mybatis-plus模板生成-->
    <dependency>
        <groupId>org.apache.velocity</groupId>
        <artifactId>velocity-engine-core</artifactId>
        <version>2.2</version>
    </dependency>

    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
        <version>1.2.35</version>
    </dependency>

    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>30.0-jre</version>
    </dependency>
</dependencies>
```


### 配置文件

```yaml
server:
  port: 8081

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    jdbc-url: jdbc:mysql://localhost:3306/test_db?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true
    username: root
    password: root
```

### 生成entity, mapper

实体类
```java
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class DataCommonCache implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键，生成序列号Id
     */
    @TableId(value = "cache_id", type = IdType.AUTO)
    private Long cacheId;

    /**
     * 缓存的key 长度100 超过100的话通过编码后缩短
     */
    private String cacheKey;

    /**
     * 缓存的值
     */
    private String cacheValue;

    /**
     * 过期时间
     */
    private Date cacheExpire;
}
```

mapper, 包含自定义SQL

```java
@Mapper
public interface DataCommonCacheMapper extends BaseMapper<DataCommonCache> {
    /**
     * 根据缓存key查询缓存ID 如果ID为空 表示缓存不存在 不为空表示缓存存在
     * @param cacheKey 缓存key
     * @return 缓存的ID, 过期时间 为空表示缓存不存在
     */
    DataCommonCache selectIdByKey(@Param("cacheKey") String cacheKey);

    /**
     * 根据缓存key查询缓存ID 如果ID为空 表示缓存不存在 不为空表示缓存存在
     * @param cacheKey 缓存key
     * @return 缓存的ID, 过期时间，缓存值 为空表示缓存不存在
     */
    DataCommonCache selectByKey(@Param("cacheKey") String cacheKey);

    /**
     * 根据缓存key查询缓存ID 如果ID为空 表示缓存不存在 不为空表示缓存存在
     * @param cacheKey 缓存key
     * @return 缓存的ID 为空表示
     */
    String selectValueByKey(@Param("cacheKey") String cacheKey);

    /**
     * 根据缓存key删除数据
     * @param cacheKey 缓存key
     */
    int deleteByCacheKey(@Param("cacheKey") String cacheKey);
}
```

mapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.itdl.mapper.DataCommonCacheMapper">
    <delete id="deleteByCacheKey" parameterType="java.lang.String">
        delete from data_common_cache where cache_key = #{cacheKey}
    </delete>


    <!--根据缓存key获取缓存信息，主要包含缓存ID和过期时间 结果为空表示没有数据-->
    <select id="selectIdByKey" parameterType="string" resultType="com.itdl.entity.DataCommonCache">
        select cache_id, cache_expire from data_common_cache where cache_key = #{cacheKey} limit 1
    </select>

    <!--包含缓存值-->
    <select id="selectByKey" parameterType="string" resultType="com.itdl.entity.DataCommonCache">
        select cache_id, cache_value, cache_expire from data_common_cache where cache_key = #{cacheKey} limit 1
    </select>

    <!--根据缓存的key获取缓存的值-->
    <select id="selectValueByKey" parameterType="string" resultType="java.lang.String">
        select cache_value from data_common_cache where cache_key = #{cacheKey} limit 1
    </select>
</mapper>

```

### 整合MybatisPlus

整合MybatisPlus用于增删改差， 并实现了MybtaisPlus真正的批量新增和批量修改

#### 数据源配置类DatasourceConfig

包含了数据源的配置和SqlSessionFactory配置，且注入了MybatisPlus的配置

```java
/**
 * @Description 数据库相关配置
 * @Author itdl
 * @Date 2022/08/10 09:20
 */
@Configuration
@MapperScan(basePackages = "com.itdl.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class DatasourceConfig {


    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() throws SQLException {
        return DataSourceBuilder.create().build();
    }


    @Bean("easySqlInjector")
    public EasySqlInjector easySqlInjector() {
        return new EasySqlInjector();
    }


    @Bean
    public GlobalConfig globalConfig(EasySqlInjector easySqlInjector){
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setSqlInjector(easySqlInjector);
        return globalConfig;
    }

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource, GlobalConfig globalConfig) throws Exception {

        MybatisSqlSessionFactoryBean sessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);
        sessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().
                getResources("classpath*:mapper/**/*.xml"));
        sessionFactoryBean.setPlugins(new PaginationInterceptor());

        //添加自定义sql注入接口
        sessionFactoryBean.setGlobalConfig(globalConfig);//添加自定义sql注入接口
        return sessionFactoryBean.getObject();
    }

}
```

#### Mybatis 批量插入/更新配置

```java
public class EasySqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass) {
        List<AbstractMethod> methodList = super.getMethodList(mapperClass);
        methodList.add(new InsertBatchSomeColumn());
        methodList.add(new UpdateBatchMethod());
        return methodList;
    }

}

/**
 * 批量更新方法实现，条件为主键，选择性更新
 */
@Slf4j
public class UpdateBatchMethod extends AbstractMethod {
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        String sql = "<script>\n<foreach collection=\"list\" item=\"item\" separator=\";\">\nupdate %s %s where %s=#{%s} %s\n</foreach>\n</script>";
        String additional = tableInfo.isWithVersion() ? tableInfo.getVersionFieldInfo().getVersionOli("item", "item.") : "" + tableInfo.getLogicDeleteSql(true, true);
        String setSql = sqlSet(false, false, tableInfo, false, "item", "item.");
        String sqlResult = String.format(sql, tableInfo.getTableName(), setSql, tableInfo.getKeyColumn(), "item." + tableInfo.getKeyProperty(), additional);
        //log.debug("sqlResult----->{}", sqlResult);
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, sqlResult, modelClass);
        // 第三个参数必须和RootMapper的自定义方法名一致
        return this.addUpdateMappedStatement(mapperClass, modelClass, "updateBatch", sqlSource);
    }
}
```

### 缓存实现

#### 实现思路

* 1、配置一个可调度的线程池，用于异步队列的调度
* 2、编写一个基础调度父类，实现调度的基本逻辑
* 3、编写缓存插入，覆盖，删除的调度逻辑
* 4、将调度逻辑整合为一个缓存工具类
* 5、使用Controller接口测试缓存增删改查

#### 配置可调度的线程池

```java
/**
 * @Description 通用配置及
 * @Author itdl
 * @Date 2022/08/09 17:57
 */
@Configuration
public class CommonConfig {
    @Bean("scheduledThreadPoolExecutor")
    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
        //线程名
        String threadNameStr = "统一可调度线程-%d";
        //线程工厂类就是将一个线程的执行单元包装成为一个线程对象，比如线程的名称,线程的优先级,线程是否是守护线程等线程；
        // guava为了我们方便的创建出一个ThreadFactory对象,我们可以使用ThreadFactoryBuilder对象自行创建一个线程.
        ThreadFactory threadNameVal = new ThreadFactoryBuilder().setNameFormat(threadNameStr).build();
        // 单线程池
        return new ScheduledThreadPoolExecutor(
                // 核心线程池
                4,
                // 最大线程池
                threadNameVal,
                // 使用策略为抛出异常
                new ThreadPoolExecutor.AbortPolicy());
    }
}
```

#### 编写可调度的公共父类，实现调度的基本逻辑

```java
@Slf4j
public abstract class BaseCacheHelper<T> {
    @Resource
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    // 队列
    private final BlockingQueue<T> QUEUE = new ArrayBlockingQueue<>(1024);
    // listener执行次数 计数器
    private final AtomicInteger EXECUTE_COUNT = new AtomicInteger();
    // 事件集合
    private final List<T> eventStorageList = Collections.synchronizedList(new ArrayList<>());

    /**
     * 判断队列是否为空
     */
    public boolean checkQueueIsEmpty() {
        return QUEUE.isEmpty();
    }

    /**
     * 入队方法
     * @param datas 批量入队
     */
    public void producer(List<T> datas) {
        for (T data : datas) {
            producer(data);
        }
    }

    /**
     * 入队方法
     * @param data 单个入队
     */
    public void producer(T data) {
        try {
            if (QUEUE.contains(data)){
                return;
            }
            // 入队 满了则等待
            QUEUE.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("================>>>通用队列：{}：当前队列存在数据：{}", this.getClass().getName(), QUEUE.size());
    }


    @PostConstruct
    public void consumer() {
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
            try {
                // 队列数量达到指定消费批次数量
                if (EXECUTE_COUNT.get() >= getBatchSize()) {
                    doConsumer();
                } else {
                    while (EXECUTE_COUNT.get() < getBatchSize() && QUEUE.size() != 0) {
                        // 加入事件
                        final T take = QUEUE.take();
                        eventStorageList.add(take);
                        EXECUTE_COUNT.incrementAndGet();
                    }

                    // 队列为空了  同样需要处理，及时没有满
                    if (EXECUTE_COUNT.get() < getBatchSize() && QUEUE.size() == 0) {
                        doConsumer();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 3000, getPeriodTime(), TimeUnit.MILLISECONDS);
    }

    /**
     * 消费数据
     */
    protected void doConsumer() {
        // 这里面开始真正的写磁盘
        if (ObjectUtils.isEmpty(eventStorageList)) {
            return;
        }
        // 批处理
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("=========>>>>消费数据{}条", eventStorageList.size());
        for (T t : eventStorageList) {
            StopWatch subStopWatch = new StopWatch();
            subStopWatch.start();
            // 处理每一个消费者的逻辑 用于子类实现
            doHandleOne(t);
            subStopWatch.stop();
        }
        // 重置数据
        EXECUTE_COUNT.set(0);
        eventStorageList.clear();
        stopWatch.stop();
        log.info("=========>>>>通用队列：{}：消费完成，总耗时：{}s<<<<=========", this.getClass().getName(), String.format("%.4f", stopWatch.getTotalTimeSeconds()));
    }

    /**
     * 消费一条数据
     *
     * @param data 处理数据
     */
    protected abstract void doHandleOne(T data);


    /**
     * 批次大小 默认每次消费100条
     *
     * @return 此次大小
     */
    protected Integer getBatchSize() {
        return 100;
    }

    /**
     * 批次大小 执行完任务后 间隔多久再执行 单位 毫秒 默认5秒
     *
     * @return 此次大小
     */
    protected Integer getPeriodTime() {
        return 1000;
    }
}
```

### 增删改调度任务实现

```java
@Slf4j
@Component
public class DbCacheHelper{
    @Slf4j
    @Component
    public static class InsertCache extends BaseCacheHelper<DataCommonCache>{
        @Autowired
        private DataCommonCacheMapper dataCommonCacheMapper;

        @Override
        protected void doHandleOne(DataCommonCache data) {
            final String cacheKey = data.getCacheKey();
            log.info("=====================开始插入缓存数据cacheKey:{}===========================", cacheKey);
            try {
                dataCommonCacheMapper.insert(data);
            } catch (Exception e) {
                log.error("=======>>>>插入缓存数据失败：{}", e.getMessage());
                e.printStackTrace();
            }
            log.info("=====================完成插入缓存数据cacheKey:{}===========================", cacheKey);
        }
    }


    @Slf4j
    @Component
    public static class UpdateCache extends BaseCacheHelper<DataCommonCache>{
        @Autowired
        private DataCommonCacheMapper dataCommonCacheMapper;
        @Override
        protected void doHandleOne(DataCommonCache data) {
            final String cacheKey = data.getCacheKey();
            log.info("=====================开始覆盖写入缓存数据cacheKey:{}===========================", cacheKey);
            try {
                dataCommonCacheMapper.updateById(data);
            } catch (Exception e) {
                log.error("=======>>>>覆盖写入缓存数据失败：{}", e.getMessage());
                e.printStackTrace();
            }
            log.info("=====================完成覆盖写入缓存数据cacheKey:{}===========================", cacheKey);
        }
    }


    @Slf4j
    @Component
    public static class DeleteCache extends BaseCacheHelper<String>{
        @Autowired
        private DataCommonCacheMapper dataCommonCacheMapper;
        @Override
        protected void doHandleOne(String cacheKey) {
            log.info("=====================开始删除缓存数据cacheKey:{}===========================", cacheKey);
            try {
                dataCommonCacheMapper.deleteByCacheKey(cacheKey);
            } catch (Exception e) {
                log.error("=======>>>>删除缓存数据失败：{}", e.getMessage());
                e.printStackTrace();
            }
            log.info("=====================完成删除写入缓存数据cacheKey:{}===========================", cacheKey);
        }
    }
}
```

#### 缓存工具类编写

在工具类里面实现缓存的逻辑

新增缓存思路

* 1、根据缓存key查询缓存ID和过期时间
* 2、结果为空，表示没有缓存，发送数据到缓存队列，等待新增缓存队列任务调度
* 3、结果不为空，继续判断过期时间，过期时间不为空，并且已经过期了，则发送到过期删除队列，等待调度
* 4、没有过期，真正查询缓存的值, 比较值是是否更新，更新了发送更新数据到更新队列，没更新则不管

查询缓存思路

* 1、根据缓存key查询数据(包含缓存值)
* 2、结果为空，表示缓存不存在，直接返回null
* 3、结果不为空，判断是否过期，过期则发送过期删除到删除队列
* 4、返回查询结果

删除缓存思路

* 1、根据缓存key查询数据(不包含缓存值)
* 2、为空则不需要删除，不为空发送到删除队列，等待调度


代码实现

```java
@Component
@Slf4j
public class DbCacheUtil {

    @Autowired
    private DataCommonCacheMapper dataCommonCacheMapper;

    @Autowired
    private DbCacheHelper.InsertCache insertCache;
    @Autowired
    private DbCacheHelper.UpdateCache updateCache;
    @Autowired
    private DbCacheHelper.DeleteCache deleteCache;

    /**
     * 插入缓存数据
     * @param cacheKey 缓存key
     * @param cacheValue 缓存值
     * @param ttl 单位毫秒 缓存失效时间 小于0表示永不过期
     */
    public synchronized void putCache(String cacheKey, String cacheValue, long ttl){
        // 根据缓存key查询缓存  ID/过期时间等
        final DataCommonCache cache = dataCommonCacheMapper.selectIdByKey(cacheKey);
        if (cache == null){
            // 新增数据
            DataCommonCache commonCache = buildInsertData(cacheKey, cacheValue, ttl);
            // 发送给入库队列
            insertCache.producer(commonCache);
            return;
        }

        // 缓存设置了过期时间 并且缓存国企时间比当前时间小(过期了)
        if (cache.getCacheExpire() != null && cache.getCacheExpire().getTime() < System.currentTimeMillis()){
            // 发送删除过期Key队列
            deleteCache.producer(cacheKey);
            return;
        }

        // 都不是 表示需要覆盖缓存 也就是更新缓存

        // 先判断缓存的值是否和数据库的值一致 一致则无需覆盖
        final String cacheValueResult = dataCommonCacheMapper.selectValueByKey(cacheKey);
        if (StringUtils.equals(cacheValueResult, cacheValue)){
            log.info("=============>>>>缓存key：{}的value与数据库一致，无需覆盖", cacheValue);
            return;
        }

        // 发送一个覆盖的请求
        final DataCommonCache dataCommonCache = buildInsertData(cacheKey, cacheValue, ttl);
        dataCommonCache.setCacheId(cache.getCacheId());
        updateCache.producer(dataCommonCache);
    }


    /**
     * 根据缓存从数据库查询
     * @param cacheKey 缓存key
     * @return 缓存值cacheValue 这里返回的值可能是已过期的 知道过期key删除之后才会返回新的数据
     */
    public synchronized String getCache(String cacheKey){
        // 根据缓存key查询缓存  ID/过期时间等
        final DataCommonCache cache = dataCommonCacheMapper.selectByKey(cacheKey);
        if (cache == null){
            log.info("===========缓存不存在, 请请先调用putCache缓存===========");
            return null;
        }

        // 缓存设置了过期时间 并且缓存国企时间比当前时间小(过期了)
        if (cache.getCacheExpire() != null && cache.getCacheExpire().getTime() < System.currentTimeMillis()){
            // 发送删除过期Key队列
            deleteCache.producer(cacheKey);
            // 等待异步线程处理删除过期，但是这里还是返回缓存数据，从而减少数据库的压力，直到缓存删除后再次查询到结果在返回
        }
        log.info("================命中缓存cacheKey为：{}=================", cacheKey);
        // 不为空，返回数据库
        return cache.getCacheValue();
    }


    /**
     * 根据key删除缓存
     * @param cacheKey 缓存key
     */
    public synchronized void deleteCache(String cacheKey){
        // 根据缓存key查询缓存  ID/过期时间等
        final DataCommonCache cache = dataCommonCacheMapper.selectIdByKey(cacheKey);
        if (cache == null){
            log.info("===========缓存不存在 无需删除===========");
            return;
        }

        // 发送删除消息到队列
        deleteCache.producer(cacheKey);
    }


    private DataCommonCache buildInsertData(String cacheKey, String cacheValue, long ttl) {
        final DataCommonCache commonCache = new DataCommonCache();
        commonCache.setCacheKey(cacheKey);
        commonCache.setCacheValue(cacheValue);
        // 失效时间为当前是时间 + ttl时间
        Date expireTime = null;
        if (ttl > 0){
            expireTime = new Date(System.currentTimeMillis() + ttl);
        }
        commonCache.setCacheExpire(expireTime);
        return commonCache;
    }
}
```

#### 缓存测试接口

```java
@RestController
@RequestMapping("/dbCache")
public class DbCacheController {
    @Autowired
    private DbCacheUtil dbCacheUtil;

    /**缓存时间设置为5分钟, 可以自行设置*/
    private static final Long ttl = 300 *  1000L;

    @GetMapping("/test/putCache")
    public String putCache(@RequestParam("cacheKey") String cacheKey, @RequestParam("cacheValue")  String cacheValue){
        dbCacheUtil.putCache(cacheKey, cacheValue, ttl);
        return "success";
    }

    @GetMapping("/test/getCache")
    public String getCache(@RequestParam("cacheKey") String cacheKey){
        return dbCacheUtil.getCache(cacheKey);
    }

    @GetMapping("/test/deleteCache")
    public String deleteCache(@RequestParam("cacheKey") String cacheKey){
        dbCacheUtil.deleteCache(cacheKey);
        return "success";
    }
}
```

#### 接口测试

新增接口：http://localhost:8081/dbCache/test/putCache?cacheKey=test_name&cacheValue=张三

查询接口：http://localhost:8081/dbCache/test/getCache?cacheKey=test_name

删除接口：http://localhost:8081/dbCache/test/deleteCache?cacheKey=test_name

测试结果

```html
2022-08-10 09:31:36.699  INFO 19572 --- [nio-8081-exec-2] com.itdl.cache.util.DbCacheUtil          : ===========缓存不存在, 请请先调用putCache缓存===========
2022-08-10 09:31:39.815  INFO 19572 --- [nio-8081-exec-3] com.itdl.cache.BaseCacheHelper           : ================>>>通用队列：com.itdl.cache.DbCacheHelper$InsertCache：当前队列存在数据：1
2022-08-10 09:31:40.812  INFO 19572 --- [      统一可调度线程-1] com.itdl.cache.BaseCacheHelper           : =========>>>>消费数据1条
2022-08-10 09:31:40.813  INFO 19572 --- [      统一可调度线程-1] c.itdl.cache.DbCacheHelper$InsertCache   : =====================开始插入缓存数据cacheKey:test_name===========================
2022-08-10 09:31:40.851  INFO 19572 --- [      统一可调度线程-1] c.itdl.cache.DbCacheHelper$InsertCache   : =====================完成插入缓存数据cacheKey:test_name===========================
2022-08-10 09:31:40.852  INFO 19572 --- [      统一可调度线程-1] com.itdl.cache.BaseCacheHelper           : =========>>>>通用队列：com.itdl.cache.DbCacheHelper$InsertCache：消费完成，总耗时：0.0383s<<<<=========
2022-08-10 09:31:42.296  INFO 19572 --- [nio-8081-exec-4] com.itdl.cache.util.DbCacheUtil          : ================命中缓存cacheKey为：test_name=================
2022-08-10 10:18:51.256  INFO 19572 --- [nio-8081-exec-8] com.itdl.cache.BaseCacheHelper           : ================>>>通用队列：com.itdl.cache.DbCacheHelper$DeleteCache：当前队列存在数据：1
2022-08-10 10:18:51.882  INFO 19572 --- [      统一可调度线程-1] com.itdl.cache.BaseCacheHelper           : =========>>>>消费数据1条
2022-08-10 10:18:51.882  INFO 19572 --- [      统一可调度线程-1] c.itdl.cache.DbCacheHelper$DeleteCache   : =====================开始删除缓存数据cacheKey:test_name===========================
2022-08-10 10:18:51.890  INFO 19572 --- [      统一可调度线程-1] c.itdl.cache.DbCacheHelper$DeleteCache   : =====================完成删除写入缓存数据cacheKey:test_name===========================
2022-08-10 10:18:51.890  INFO 19572 --- [      统一可调度线程-1] com.itdl.cache.BaseCacheHelper           : =========>>>>通用队列：com.itdl.cache.DbCacheHelper$DeleteCache：消费完成，总耗时：0.0079s<<<<=========
2022-08-10 10:19:01.817  INFO 19572 --- [nio-8081-exec-9] com.itdl.cache.util.DbCacheUtil          : ===========缓存不存在, 请请先调用putCache缓存===========

```


































