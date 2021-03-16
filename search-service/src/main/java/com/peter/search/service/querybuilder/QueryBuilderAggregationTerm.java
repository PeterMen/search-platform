package com.peter.search.service.querybuilder;

import com.alibaba.fastjson.JSON;
import com.peter.search.pojo.AggregationParam;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.InternalOrder;
import org.springframework.stereotype.Service;


/**
 * 聚合查询构造器
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "AGGREGATION")
public class QueryBuilderAggregationTerm extends QueryBuilderAggregation {

    public AggregationParam getAggParam(String paramValue){
        return JSON.parseObject(paramValue, AggregationParam.class);
    }

    public AbstractAggregationBuilder getAbstractAggregationBuilder(String serviceTag, AggregationParam aggParam, String field){
        // 设置排序
        BucketOrder order = InternalOrder.CompoundOrder.count(false);
        if(StringUtils.equals(aggParam.getOrder(), DESC)){
            order = InternalOrder.CompoundOrder.count(false);
        } else if(StringUtils.equals(aggParam.getOrder(), ASC)){
            order = InternalOrder.CompoundOrder.count(true);
        }

        // 设置聚合字段
        return AggregationBuilders.terms(field)
                .size(aggParam.getFetchSize())
                .field(getESName(serviceTag, field))
                .order(order);
    }
}
