package com.peter.search.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.dto.SuggestRequest;
import com.peter.search.entity.Constant;
import com.peter.search.service.SuggestService;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.service.querybuilder.QueryBuilderFactory;
import com.peter.search.service.responsewrap.QueryRespWrapFactory;
import com.peter.search.util.PropertyUtils;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.suggest.Suggest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 查询入口
 *
 * @author 七星
 * */
@Service
public class SuggestServiceImpl implements SuggestService {
    
    private static final Logger logger = LoggerFactory.getLogger(SuggestServiceImpl.class);

    @Autowired
    private SearchServiceImpl searchService;

    @Autowired
    ESClientFactory esClientFactory;

    @Autowired
    PropertyUtils properties;

    @Autowired
    QueryBuilderFactory queryBuilderFactory;

    /**
     * 提示词搜索
     *
     * serviceTag
     * suggester索引结构如下：
     * 分词字段：tokenizedWord
     * 原始值字段：originalWord
     * 值：value
     *
     *
     * @param serviceTag 搜索业务标示
     * @param suggestRequest keyword 关键字
     * @param suggestRequest fields 查询域
     * @param suggestRequest size 返回条数
     * @return 查询结果
     * */
    public SearchResultDTO suggest(String serviceTag, SuggestRequest suggestRequest){

        SearchResultDTO suggestResult  = new SearchResultDTO(SearchResultDTO.STATUS_SUCCESS);

        try{
            JSONObject queryParam = suggestRequest.getQueryParam();
            JSONObject or = new JSONObject();
            or.put("tokenizedWord", "prefix:"+suggestRequest.getKeyword());
            or.put("originalWord", "prefix:"+suggestRequest.getKeyword());
            queryParam.put("sRawORParam", or);
            queryParam.put("iRowSize", suggestRequest.getSize());
            queryParam.put("serviceTag", serviceTag);

            suggestResult = searchService.search(Constant.SERVICE_TAG_SUGGEST, queryParam);

            if(suggestResult.getReturnSize() < suggestRequest.getSize()){
                // 试下词语纠错
                SearchRequest searchRequest = queryBuilderFactory.buildSuggestRequest(
                        Constant.SERVICE_TAG_SUGGEST,
                        suggestRequest.getKeyword(),
                        Arrays.asList("tokenizedWord"),
                        suggestRequest.getSize()-suggestResult.getReturnSize());

                RestHighLevelClient client = esClientFactory.getHighLevelClient(Constant.SERVICE_TAG_SUGGEST);
                searchRequest.indices(properties.getIndexAlias(Constant.SERVICE_TAG_SUGGEST));
                searchRequest.types(properties.getTypeName(Constant.SERVICE_TAG_SUGGEST));

                // 调用ES查询数据
                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                Suggest suggestResponse = searchResponse.getSuggest();
                List<String> termWords = QueryRespWrapFactory.parseSuggestResponse(suggestResponse, suggestRequest.getSize());


                // 结果包装
                suggestResult.getData().addAll(termWords.stream().map(word -> ImmutableMap.of("originalWord", word)).collect(Collectors.toList() ));
            }
        }catch (ElasticsearchStatusException e) {
            suggestResult.setStatus(SearchResultDTO.STATUS_FAILED);
            suggestResult.setErrMsg(serviceTag + "--提示词查询异常："+ getMessage(e));
            logger.error( "{}--提示词查询异常：", serviceTag, e);
        } catch (Exception e){
            suggestResult.setStatus(SearchResultDTO.STATUS_FAILED);
            suggestResult.setErrMsg(serviceTag + "--提示词查询异常："+e.getMessage());
            logger.error( "{}--提示词查询异常：", serviceTag, e);
        }
        return suggestResult;
   }

   private String getMessage(ElasticsearchStatusException e){
        StringBuilder msg = new StringBuilder();
        msg.append(e.getMessage());
        for(Throwable throwable : e.getSuppressed()){
            msg.append(throwable.getMessage());
        }
        return msg.toString();
   }
}