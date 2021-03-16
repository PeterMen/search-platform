package com.peter.search.service;

import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.pojo.CheckResult;

/**
 * 参数校验api
 *
 * @author 七星
 * */
public interface ParamCheckService {

    /**
     * solr搜索
     * 
     * @param requestParam 请求参数
     * @return 查询结果
     * */
     CheckResult check(SearchRequestDTO requestParam);
}
