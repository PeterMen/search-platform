package com.peter.search.service;

import com.alibaba.fastjson.JSONArray;
import com.peter.search.dto.MSearchResultDTO;

/**
 * 查询入口api
 *
 * @author 七星
 * */
public interface MSearchService {

    /**
     * 搜索入口
     *
     * @param serviceTag 搜索业务标示
     * @param paramsArray 查询条件
     * @return 查询结果
     * */
     MSearchResultDTO mSearch(String serviceTag, JSONArray paramsArray);
}
