package com.peter.search.service.querybuilder;

import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.stereotype.Service;

/**
 * 高亮字段构造器
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "HL")
public class QueryBuilderHL extends BaseQueryBuilder implements QueryBuilder {

    /**
     * query构造器
     * 
     * @param esQuery esQuery query
     * @param serviceTag 业务标示
     * @param requestName 请求参数名称
     * @param requestValue 请求参数值
     * 
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String requestName, String requestValue) {

        if(StringUtils.isEmpty(requestValue)){ return; }

        // 获取默认的高亮字段
        String[] highLightFieldArrays = requestValue.split(FIELD_SPLIT_STR);

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        for(String s : highLightFieldArrays){
            highlightBuilder.field(s);
        }
        esQuery.setHighlightBuilder(highlightBuilder);
    }
}
