package com.peter.search.service.querybuilder;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 复合FQ解析器--and
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "MULTIPLE_AND")
public class QueryBuilderMultipleAND extends BaseQueryBuilder implements QueryBuilder {

    @Autowired
    QueryBuilderFactory queryBuilderFactory;

    /**
     * 复合FQ解析器：value 格式为json
     *
     * @param esQuery solr query
     * @param serviceTag 业务标示
     * @param paramName 请求参数名称
     * @param paramValue 请求参数值
     *
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String paramName, String paramValue){

        if(StringUtils.isEmpty(paramValue)){
            return;
        }

        if(paramValue.startsWith(JSON_ARRAY_PREFIX)){
            // value是数组
            JSON.parseArray(paramValue).forEach(p -> {
                if(p instanceof JSONObject){
                    // 调用构造器工厂进行query build
                    QueryParam subEsQuery = new QueryParam();
                    queryBuilderFactory.buildProvidedQuery(serviceTag, (JSONObject)p, subEsQuery);
                    ignoreNullFilter(esQuery, subEsQuery);
                }
            });
        } else {

            // 调用构造器工厂进行query build
            QueryParam subEsQuery = new QueryParam();
            queryBuilderFactory.buildProvidedQuery(serviceTag, JSON.parseObject(paramValue), subEsQuery);
            ignoreNullFilter(esQuery, subEsQuery);
        }
    }

    private void ignoreNullFilter(QueryParam esQuery, QueryParam subEsQuery) {
        if(subEsQuery.getBoolQueryBuilder().filter().size() > 0){
            esQuery.getBoolQueryBuilder().filter(subEsQuery.getBoolQueryBuilder());
        }
    }
}
