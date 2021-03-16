package com.peter.search.service.querybuilder;

import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.stereotype.Service;

/**
 * query构造器--范围结束
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "RANGE_END")
public class QueryBuilderRangeEnd extends BaseQueryBuilder implements QueryBuilder {

    /**
     * query构造器--范围结束
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
                    new RangeQueryBuilder(esName).lte(paramValue), ScoreMode.Total));
        } else {
            boolqueryBuilder.filter(new RangeQueryBuilder(esName).lte(paramValue));
        }
    }
}
