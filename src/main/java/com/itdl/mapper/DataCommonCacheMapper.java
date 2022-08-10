package com.itdl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itdl.entity.DataCommonCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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