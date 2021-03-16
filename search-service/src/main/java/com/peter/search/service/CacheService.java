package com.peter.search.service;

import com.peter.search.dto.SearchRequestDTO;

/**
 * 参数校验api
 *
 * @author 七星
 * */
public interface CacheService {

    /**
     * key生成规则：es + serviceTag + md5
     *
     * @param  requestParam 查询参数
     * @param tClass class
     *  @return 查询结果
     * */
    <T> T getFromCache(SearchRequestDTO requestParam, final Class<T> tClass);

     /**
      * 将查询结果保存到cache
      * @param requestParam 查询参数
      * @param searchResult 查询结果
      * */
     void saveToCache(SearchRequestDTO requestParam, Object searchResult);
}
