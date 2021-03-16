package com.peter.search.builder;

import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.pojo.AggregationParam;
import com.peter.search.pojo.GeoParam;
import com.peter.search.pojo.RangeAggregation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class BuilderTest {

    @Test
    public void testBuilder(){
        // 创建构造器
        SearchRequestBuilder builder = new SearchRequestBuilder();

        // and逻辑
        SearchRequestBuilder.AndBuilder andBuilder = new SearchRequestBuilder.AndBuilder();
        andBuilder.fq("code", "123").fq("size", 123);

        // and逻辑
        SearchRequestBuilder.AndBuilder andBuilder2 = new SearchRequestBuilder.AndBuilder();
        andBuilder2.fq("code", "456").fq("size", 456);

        // and逻辑
        SearchRequestBuilder.AndBuilder andBuilder3 = new SearchRequestBuilder.AndBuilder();
        andBuilder2.fq("code", "789").fq("size", 789);

        // or逻辑
        SearchRequestBuilder.ORBuilder orBuilder = new SearchRequestBuilder.ORBuilder();
        orBuilder.fq("attrCode", "38883")
                .fq("attrCode", "33,32,22")
                .addAnd(andBuilder)
                .addAnd(andBuilder2);

        // 聚合查询
        AggregationParam aggregationParam = new AggregationParam();
        aggregationParam.setField("salePrice");

        // 聚合查询
        AggregationParam aggregationParam2 = new AggregationParam();
        aggregationParam2.setField("attrCode");
        aggregationParam2.setFunction(AggregationParam.FUNCTION_ENUM.avg);

        // 聚合查询
        RangeAggregation rangeAggregation = new RangeAggregation();
        rangeAggregation.setField("salePrice");
        rangeAggregation.setRangeType(RangeAggregation.RangeType.general);
        rangeAggregation.setRanges(Arrays.asList(new RangeAggregation.Range()));

        // 构造最终查询对象
        SearchRequestDTO<JSONObject> searchRequest = builder.serviceTag("item") // 指定业务标识
                .sort("salePrice desc")  // 指定排序条件
                .addOR(orBuilder)  // 添加or逻辑
                .addAnd(andBuilder3) // 添加and逻辑
                .addAggregation(aggregationParam) // 添加聚合查询
                .addAggregation(aggregationParam2)
                .fq("roomId", "5242", FQ_RULE.WILDCARD) // fq查询，指定fq规则为：wildcard
                .fq("listTime", "525652", FQ_RULE.GT) // fq查询，指定fq规则为：gt
                .reScoreId("4568225") // 指定二次排序模型的id
                .fq("size2", 4,  FQ_RULE.AND ) // fq查询，指定fq规则为：gt
                .iRowSize(20) // 指定返回条数
                .iStart(10)  // 指定起始行
                .fl("title,subTitle")  // 指定返回字段
                .sKw("title^0.7,subTitle^0.3", "短袖") // 指定关键字查询
                .addRangeAgg(rangeAggregation)
                .docIds(Arrays.asList("5122", "653"))
                .sortMode("sdfsd")
                .routing("sdfsdf")
                .addGeo(new GeoParam())
                .build();

        System.out.println(searchRequest.getQueryParam().toJSONString());
        System.out.println(searchRequest.getSearchConfig());
    }
}
