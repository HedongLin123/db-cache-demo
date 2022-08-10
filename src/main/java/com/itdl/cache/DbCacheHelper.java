package com.itdl.cache;

import com.itdl.entity.DataCommonCache;
import com.itdl.mapper.DataCommonCacheMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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