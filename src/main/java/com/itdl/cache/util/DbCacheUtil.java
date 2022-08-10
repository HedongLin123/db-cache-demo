package com.itdl.cache.util;

import com.itdl.cache.DbCacheHelper;
import com.itdl.entity.DataCommonCache;
import com.itdl.mapper.DataCommonCacheMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

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