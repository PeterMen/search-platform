package com.peter.search.service.querybuilder;

import com.alibaba.fastjson.JSON;
import com.peter.search.pojo.AggregationParam;
import com.peter.search.pojo.RangeAggregation;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.IpRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;


/**
 * 聚合查询构造器
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "RANGE_AGGREGATION")
public class QueryBuilderAggregationRange extends QueryBuilderAggregation {

    public RangeAggregation getAggParam(String paramValue){
        return JSON.parseObject(paramValue, RangeAggregation.class);
    }

    public AbstractAggregationBuilder getAbstractAggregationBuilder(String serviceTag, AggregationParam aggParam, String field) {

        RangeAggregation agg = (RangeAggregation) aggParam;
        Assert.notEmpty(agg.getRanges(), "ranges不能为空");
        Assert.notNull(agg.getRangeType(), "rangeType不支持");

        if(agg.getRangeType() == RangeAggregation.RangeType.general){
            // 通用聚合查询
            RangeAggregationBuilder aggregationBuilder = AggregationBuilders.range(field)
                    .field(getESName(serviceTag, field));
            agg.getRanges().forEach(range -> {
                if(StringUtils.isNotEmpty(range.getKey())){
                    aggregationBuilder.addRange(range.getKey(),
                            StringUtils.isEmpty(range.getFrom())?null:Double.parseDouble(range.getFrom()),
                            StringUtils.isEmpty(range.getTo())?null:Double.parseDouble(range.getTo()));
                } else {
                    aggregationBuilder.addRange(
                            StringUtils.isEmpty(range.getFrom())?null:Double.parseDouble(range.getFrom()),
                            StringUtils.isEmpty(range.getTo())?null:Double.parseDouble(range.getTo()));
                }
            });
            return aggregationBuilder;
        } else if(agg.getRangeType() == RangeAggregation.RangeType.date_range){
            DateRangeAggregationBuilder aggregationBuilder = AggregationBuilders.dateRange(field)
                    .field(getESName(serviceTag, field))
                    .format(agg.getFormat());
            agg.getRanges().forEach(range -> {
                if(StringUtils.isNotEmpty(range.getKey())){
                    aggregationBuilder.addRange(range.getKey(), range.getFrom(), range.getTo());
                } else {
                    aggregationBuilder.addRange(range.getFrom(), range.getTo());
                }
            });
            return aggregationBuilder;
        } else if(agg.getRangeType() == RangeAggregation.RangeType.ip_range){
            IpRangeAggregationBuilder aggregationBuilder = AggregationBuilders.ipRange(field)
                    .field(getESName(serviceTag, field));
            agg.getRanges().forEach(range -> {
                if(StringUtils.isNotEmpty(range.getKey())){
                    aggregationBuilder.addRange(range.getKey(), range.getFrom(), range.getTo());
                } else {
                    aggregationBuilder.addRange(range.getFrom(), range.getTo());
                }
            });
            return aggregationBuilder;
        }

        return null;
    }
}
