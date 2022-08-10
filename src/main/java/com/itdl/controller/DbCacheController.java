package com.itdl.controller;

import com.itdl.cache.util.DbCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dbCache")
public class DbCacheController {
    @Autowired
    private DbCacheUtil dbCacheUtil;

    /**缓存时间设置为5分钟*/
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