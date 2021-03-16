package com.peter.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.service.CacheService;
import com.peter.search.util.PropertyUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * cache相关服务
 *
 * @author 王海涛
 * */
@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String EXPIRE_TIME_SUFFIX = "redis.expire.second";

    @Autowired
    PropertyUtils properties;
    /**
     * key生成规则：es + serviceTag + md5
     *
     * */
    @Override
    public <T> T getFromCache(SearchRequestDTO requestParam, final Class<T> tClass)  {
        
        String serviceTag = requestParam.getServiceTag();
        String params =  JSON.toJSONString(requestParam.getQueryParam());

        return (T) redisTemplate.opsForValue().get("es-" + serviceTag + DigestUtils.md5Hex(params));
    }

    @Override
    public void saveToCache(SearchRequestDTO requestParam, Object searchResult) {

        String params = JSON.toJSONString(requestParam.getQueryParam());
        String serviceTag = requestParam.getServiceTag();

        String redisKey = "es-" + serviceTag + DigestUtils.md5Hex(params);

        String expireTimeSeconds = properties.getProperty(EXPIRE_TIME_SUFFIX + "." + serviceTag, "5");
        redisTemplate.opsForValue().set(redisKey, searchResult, Long.valueOf(expireTimeSeconds), TimeUnit.SECONDS);
    }
}
