package com.peter.search.service.querybuilder;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 复合FQ解析器--OR
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "MULTIPLE_OR")
public class QueryBuilderMultipleOR extends BaseQueryBuilder implements QueryBuilder {

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

        // 默认bool中的条件最少要满足一项，但是假如一项都不满足，就需要用should
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.minimumShouldMatch(getMininumShouldMatch(paramName, serviceTag));

        if(paramValue.startsWith(JSON_ARRAY_PREFIX)){
            // value是数组
            JSON.parseArray(paramValue).forEach(p -> {
                if(p instanceof JSONObject){
                    // 调用构造器工厂进行query build
                    QueryParam subEsQuery = new QueryParam();
                    queryBuilderFactory.buildProvidedQuery(serviceTag, (JSONObject)p, subEsQuery);
                    if(subEsQuery.getBoolQueryBuilder().filter().size() > 0){
                        boolQueryBuilder.should(subEsQuery.getBoolQueryBuilder());
                    }

                }
            });

        } else {

            // 非数组，直接调用构造器工厂进行query build
            QueryParam subEsQuery = new QueryParam();
            queryBuilderFactory.buildProvidedQuery(serviceTag, JSON.parseObject(paramValue), subEsQuery);

            // and关系转换成或关系拼接
            subEsQuery.getBoolQueryBuilder().filter().forEach(queryBuilder -> boolQueryBuilder.should(queryBuilder));
        }
        if(boolQueryBuilder.should().size() > 0){
            esQuery.getBoolQueryBuilder().filter(boolQueryBuilder);
        }
    }
}
