package com.itdl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

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