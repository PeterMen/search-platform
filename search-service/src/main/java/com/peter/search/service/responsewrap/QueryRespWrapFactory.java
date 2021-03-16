package com.peter.search.service.responsewrap;

import com.peter.search.dto.SearchResultDTO;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.max.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.min.ParsedMin;
import org.elasticsearch.search.aggregations.metrics.sum.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.tophits.ParsedTopHits;
import org.elasticsearch.search.suggest.Suggest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询结果包装工厂
 *
 * @author 王海涛
 * */
public class QueryRespWrapFactory {

    private QueryRespWrapFactory(){}

    /**
     * 封装Solr Query response
     * 
     * @param searchResponse 响应结果
     * @param searchResult 最终返回对象
     * @return baseResult
     * @throws Exception
     **/
    public static void wrapQueryResponse(SearchResponse searchResponse, SearchResultDTO searchResult){

        // 设置search结果
        ArrayList resultList = new ArrayList();
        for(SearchHit hit : searchResponse.getHits().getHits()){
            Map m = hit.getSourceAsMap();
            m.put("docId", hit.getId());
            resultList.add(m);
        }
        searchResult.setData(resultList);

        // 组装聚合结果
        if(searchResponse.getAggregations() != null){
            handleAggregations(searchResult, searchResponse.getAggregations());
        }
    }

    /**
     * nested结果解析
     * */
    private static void handleAggregations(SearchResultDTO searchResult, Aggregations aggs) {
        Iterator<Aggregation> teamBucketIt = aggs.asList().iterator();
        while (teamBucketIt.hasNext()){
            Aggregation subAgg = teamBucketIt.next();
            if(subAgg instanceof ParsedTerms){
                handleTermsAggregation(searchResult, (ParsedTerms)subAgg);
            } else if(subAgg instanceof ParsedRange){
                handleRangeAggregation(searchResult, (ParsedRange)subAgg);
            } else if(subAgg instanceof ParsedTopHits){
                handleTopHits(searchResult, (ParsedTopHits)subAgg);
            } else if(subAgg instanceof ParsedFilter){
                handleAggregations(searchResult, ((ParsedFilter)subAgg).getAggregations());
            } else if(subAgg instanceof ParsedNested){
                handleAggregations(searchResult, ((ParsedNested)subAgg).getAggregations());
            }
        }
    }

    /**
     * range aggregation结果解析
     * */
    private static void handleRangeAggregation(SearchResultDTO searchResult, ParsedRange aggs) {
        Iterator<? extends  Range.Bucket>  teamBucketIt = aggs.getBuckets().iterator();
        while (teamBucketIt.hasNext()){
            SearchResultDTO.AggregationData aggregationData = new SearchResultDTO.AggregationData();
            Range.Bucket buck = teamBucketIt.next();
            aggregationData.setKey(buck.getKeyAsString());
            aggregationData.setCount(buck.getDocCount());

            List<Aggregation> subAggregations = buck.getAggregations().asList();
            handleSubAggregations(aggregationData, subAggregations);

            searchResult.addAggregationData(aggs.getName(), aggregationData);
        }
    }

    /**
     * Terms aggregation结果解析
     * */
    private static void handleTermsAggregation(SearchResultDTO searchResult, ParsedTerms aggs) {
        Iterator<? extends Terms.Bucket> teamBucketIt = aggs.getBuckets().iterator();
        while (teamBucketIt.hasNext()){
            SearchResultDTO.AggregationData aggregationData = new SearchResultDTO.AggregationData();
            Terms.Bucket buck = teamBucketIt.next();
            aggregationData.setKey(buck.getKeyAsString());
            aggregationData.setCount(buck.getDocCount());

            List<Aggregation> subAggregations = buck.getAggregations().asList();
            handleSubAggregations(aggregationData, subAggregations);

            searchResult.addAggregationData(aggs.getName(), aggregationData);
        }
    }


    /**
     * sub聚合结果解析
     *
     * */
    private static void handleSubAggregations(SearchResultDTO.AggregationData aggregationData, List<Aggregation> subAggregations) {
        for(Aggregation subAggs : subAggregations){
            if(subAggs instanceof ParsedMin){
                aggregationData.setMin(((ParsedMin) subAggs).getValue());
            } else if(subAggs instanceof ParsedSum){
                aggregationData.setSum(((ParsedSum) subAggs).getValue());
            } else if(subAggs instanceof ParsedMax){
                aggregationData.setMax(((ParsedMax) subAggs).getValue());
            } else if(subAggs instanceof ParsedAvg){
                aggregationData.setAvg(((ParsedAvg) subAggs).getValue());
            } else if(subAggs instanceof ParsedTopHits){
                for(SearchHit hit : ((ParsedTopHits) subAggs).getHits().getHits()){
                    aggregationData.setSourceData(hit.getSourceAsMap());
                }
            }
        }
    }

    /**
     * ParsedTopHits结果解析
     * */
    private static void handleTopHits(SearchResultDTO searchResult, ParsedTopHits aggs) {

        ArrayList resultList = new ArrayList();
        for(SearchHit hit : aggs.getHits().getHits()){
            resultList.add(hit.getSourceAsMap());
        }
        searchResult.setData(resultList);
    }

    public static List<String> parseSuggestResponse(Suggest suggestResponse, int maxSize) {

        if(suggestResponse == null) return Arrays.asList();

        String[] suggestWords = new String[maxSize];
        float[] scores = new float[maxSize];

        suggestResponse.iterator().forEachRemaining(entries -> {
            entries.getEntries().forEach(s -> {
                s.forEach(option -> {
                    for(int j=0; j < scores.length; j++){
                        if(option.getScore() > scores[j]){
                            scores[j] = option.getScore();
                            suggestWords[j] = option.getText().string();
                            break;
                        }
                    }
                });
            });
        });
        return Arrays.stream(suggestWords).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
    }
}
