package com.peter.search.service.querybuilder;


import com.peter.search.service.querybuilder.queryparam.QueryParam;

/**
 * query构造器基类
 * 
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
public interface QueryBuilder {
    

    /**
     * query构造器
     * 
     * @param esQuery solr query
     * @param serviceTag 业务标示
     * @param requestName 请求参数名称
     * @param requestValue 请求参数值
     * */
    void buildQuery(QueryParam esQuery, String serviceTag, String requestName, String requestValue);
}
