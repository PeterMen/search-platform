package com.peter.search.service.querybuilder;

import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

/**
 * query构造器--前缀匹配
 *
 * @author 七星
 * @date 2018年11月06日
 * @version 1.0
 */
@Service(value = "PREFIX")
public class QueryBuilderPrefix extends BaseQueryBuilder implements QueryBuilder {

    /**
     * query构造器--前缀匹配
     * 
     * @param esQuery  query
     * @param serviceTag 业务标示
     * @param paramName 请求参数名称
     * @param paramValue 请求参数值
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String paramName, String paramValue){

        BoolQueryBuilder boolqueryBuilder = esQuery.getBoolQueryBuilder();

        // 取es映射名称
        String esName = getESName(serviceTag, paramName);
        if(isNested(esName, serviceTag)){
            boolqueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName),
                    new PrefixQueryBuilder(esName, paramValue), ScoreMode.Total));
        } else {
            boolqueryBuilder.filter(new PrefixQueryBuilder(esName, paramValue));
        }
    }
}
