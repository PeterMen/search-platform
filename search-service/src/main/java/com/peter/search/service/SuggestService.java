package com.peter.search.service;

import com.peter.search.dto.SearchResultDTO;
import com.peter.search.dto.SuggestRequest;


/**
 * 查询入口api
 *
 * @author 七星
 * */
public interface SuggestService {

    /**
     *
     * 
     * @param  serviceTag 搜索业务标示
     * @param suggestRequest keyword 关键字
     * @param suggestRequest fields 查询域
     * @param suggestRequest size 返回条数
     * @return 查询结果
     * */
    public SearchResultDTO suggest(String serviceTag, SuggestRequest suggestRequest);
}
