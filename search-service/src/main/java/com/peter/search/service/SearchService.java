package com.peter.search.service;

import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchResultDTO;
import org.elasticsearch.action.search.SearchRequest;

import java.util.function.Consumer;

/**
 * 查询入口api
 *
 * @author 七星
 * */
public interface SearchService {

    /**
     * solr搜索
     * 
     * @param serviceTag 搜索业务标示
     * @param paramsObj 查询条件
     * @return 查询结果
     * */
    public SearchResultDTO search(String serviceTag, JSONObject paramsObj, Consumer<SearchRequest>... consumer);
}
