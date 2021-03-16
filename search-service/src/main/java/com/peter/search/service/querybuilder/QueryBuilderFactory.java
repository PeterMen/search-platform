package com.peter.search.service.querybuilder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.service.querybuilder.queryparam.QueryParam;
import com.peter.search.util.Constant;
import com.peter.search.util.PropertyUtils;
import com.peter.search.util.WebAppContextUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * 构造器工厂
 * 目前支持的query构造器 key:Q, FQ, FACET, GEO, MULTIPLE_FQ, SORT, HL, GROUP, FL, EDISMAX, POLYGON, PAGE_START, PAGE_SIZE, DF
 * 
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Component
public class QueryBuilderFactory {

    public static final String QUERY_BUILDER = "_QUERY_BUILDER";
    
    /**
     * 默认解析器
     * */
    public static final String DF_QUERY_BUILDER = "_DF_QUERY_BUILDER";

    /**
     * 查询偏好
     * */
    public static final String QUERY_SHARD_PREFERENCE_LOCAL = "_local";

    @Autowired
    PropertyUtils properties;

    /**
     * 封装ES Query
     * 
     * @param serviceTag 业务标示
     * @param paramsJson 调用方传入的参数
     * @return 查询对象构造器
     **/
    public SearchRequest buildSearchRequest(String serviceTag, JSONObject paramsJson) {

        // 创建查询参数对象
        QueryParam esQuery = new QueryParam();

        // 循环遍历查询参数，解析query
        buildProvidedQuery(serviceTag, paramsJson, esQuery);

        // 默认query构造，如分页、sKw、高亮、分组、排序
        buildDefaultQuery(serviceTag, esQuery);

        //create search request for specific indexAlias
        SearchRequest searchRequest = new SearchRequest();
        //设置查询偏好
        searchRequest.preference(QUERY_SHARD_PREFERENCE_LOCAL);
        searchRequest.indicesOptions(IndicesOptions.lenientExpandOpen());
        searchRequest.source(esQuery.getSearchSourceBuilder());
        searchRequest.routing(esQuery.getRouting());
        return searchRequest;
    }

    /**
     * 封装ES Query
     *
     * @param serviceTag 业务标示
     * @param paramsJson 调用方传入的参数
     * @return 查询对象构造器
     **/
    public DeleteByQueryRequest buildDeleteRequest(String serviceTag, JSONObject paramsJson) {

        // 创建查询参数对象
        QueryParam esQuery = new QueryParam();

        // 循环遍历查询参数，解析query
        buildProvidedQuery(serviceTag, paramsJson, esQuery);

        //create search request for specific indexAlias
        DeleteByQueryRequest deleteRequest = new DeleteByQueryRequest();
        deleteRequest.setQuery(esQuery.getBoolQueryBuilder());
        return deleteRequest;
    }

    /**
     * 构造指定的query
     * boolQuery都用and关系连接
     * */
    public void buildProvidedQuery(String serviceTag, JSONObject paramsJson, QueryParam esQuery) {
        if(paramsJson == null) return;
        Iterator<String> keyStr  = paramsJson.keySet().iterator();
        while(keyStr.hasNext()) {

            String requestName = keyStr.next();

            // 获取ES对应的key名称和query构造器
            String queryBuilderType = properties.getProperty(requestName + "_" + serviceTag + QUERY_BUILDER);
            if(ObjectUtils.isEmpty(queryBuilderType)){
                queryBuilderType = properties.getProperty(requestName + DF_QUERY_BUILDER);
                if(ObjectUtils.isEmpty(queryBuilderType)){
                    queryBuilderType = "FQ";
                }
            }

            // 从容器工厂中获取对应的query构造器，进行query build
            Object requestValueObject = paramsJson.get(requestName);
            String requestValue = paramsJson.getString(requestName);
            if(requestValueObject instanceof List || requestValueObject instanceof Map || requestValueObject instanceof Set){
                requestValue = JSON.toJSONString(requestValueObject);
            }
            if(StringUtils.isEmpty(requestValue)) continue;
            QueryBuilder baseQueryBuilder = (QueryBuilder) WebAppContextUtil.getBean(queryBuilderType);
            baseQueryBuilder.buildQuery(esQuery, serviceTag, requestName, requestValue);

            // 记录已被使用的解析器
            esQuery.getHasUsedQueryBuilderList().add(queryBuilderType);
        }
    }

    /**
     * 添加默认的解析器，如分页、sKw、高亮、分组、排序
     *
     * @param serviceTag 业务标识
     * @param esQuery 查询参数
     * */
    private void buildDefaultQuery(String serviceTag, QueryParam esQuery) {
        
        String dfQueryBuilders = properties.getProperty(serviceTag + DF_QUERY_BUILDER);
        if(StringUtils.isEmpty(dfQueryBuilders)) return;

        String[] dfQueryBuilder = dfQueryBuilders.split(",");
        List<String> dfQueryBuilderList = new ArrayList<>(Arrays.asList(dfQueryBuilder));
        dfQueryBuilderList.removeAll(esQuery.getHasUsedQueryBuilderList());

        // 循环遍历配置的默认构造器，进行默认查询配置，如果已传，则覆盖默认配置
        for(String queryBuilderType : dfQueryBuilderList){
            
            // 从容器工厂中获取对应的query构造器，进行query build
            QueryBuilder baseQueryBuilder = (QueryBuilder) WebAppContextUtil.getBean(queryBuilderType);
            baseQueryBuilder.buildQuery(esQuery, serviceTag, null, null);
        }
    }

    public SearchRequest buildSuggestRequest(String serviceTag, String keyword, List<String> fields, int size) {

        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.setGlobalText(keyword);
        if(CollectionUtils.isEmpty(fields)){
            String suggestFields = properties.getProperty(serviceTag+ Constant.DF_SUGGEST_FIELD);
            if(StringUtils.isNotEmpty(suggestFields)){
                fields = Lists.newArrayList(suggestFields.split(","));
            }
        }
        if(CollectionUtils.isEmpty(fields)) { throw new IllegalArgumentException("suggest field不能为空");}
        fields.forEach( s ->  suggestBuilder.addSuggestion(s,new TermSuggestionBuilder(s).size(size)));

        //create search request for specific indexAlias
        SearchRequest searchRequest = new SearchRequest();
        //设置查询偏好
        searchRequest.preference(QUERY_SHARD_PREFERENCE_LOCAL);
        searchRequest.indicesOptions(IndicesOptions.lenientExpandOpen());
        searchRequest.source(new SearchSourceBuilder().suggest(suggestBuilder));
        return searchRequest;
    }

}
