package com.peter.search.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.service.SearchService;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.service.querybuilder.QueryBuilderFactory;
import com.peter.search.service.responsewrap.QueryRespWrapFactory;
import com.peter.search.util.PropertyUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 查询入口
 *
 * @author 七星
 * */
@Service
public class SearchServiceImpl implements SearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    ESClientFactory esClientFactory;

    @Autowired
    PropertyUtils properties;

    @Autowired
    QueryBuilderFactory queryBuilderFactory;

    /**
     * 搜索入口
     * 
     * @param serviceTag 搜索业务标示
     * @param paramsObj 查询条件
     * @return 查询结果
     * */
    @Override
    public SearchResultDTO search(String serviceTag, JSONObject paramsObj, Consumer<SearchRequest>... consumers){
        
        SearchResultDTO searchResult = new SearchResultDTO(1);
        try{

            // 构建ES client
            RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);

            // 根据查询参数，构造ES query
            SearchRequest searchRequest = queryBuilderFactory.buildSearchRequest(serviceTag, paramsObj);
            searchRequest.indices(properties.getIndexAlias(serviceTag));
            searchRequest.types(properties.getTypeName(serviceTag));

            // query封装的参数比较重要，强制打开log debug模式，打全参数
            logger.info("{}--query封装： {}", serviceTag, searchRequest.source());

            for(Consumer c: consumers){c.accept(searchRequest);}
            // 调用ES查询数据
            SearchResponse searchResponse = client.search(searchRequest);

            // 结果包装
            QueryRespWrapFactory.wrapQueryResponse(searchResponse, searchResult);
            searchResult.setTotalNum(searchResponse.getHits().totalHits);
            searchResult.setReturnSize(searchResult.getData().size());
            searchResult.setScrollId(searchResponse.getScrollId());

        }catch (org.elasticsearch.ElasticsearchStatusException e) {
            searchResult.setStatus(SearchResultDTO.STATUS_FAILED);
            searchResult.setErrMsg(serviceTag + "--搜索异常："+ getMessage(e));
            logger.error( "{}--搜索异常：", serviceTag, e);
        } catch (Exception e){
            searchResult.setStatus(SearchResultDTO.STATUS_FAILED);
            searchResult.setErrMsg(serviceTag + "--搜索异常："+e.getMessage());
            logger.error( "{}--搜索异常：", serviceTag, e);
        }
        return searchResult;
   }

   private String getMessage(org.elasticsearch.ElasticsearchStatusException e){
        StringBuilder msg = new StringBuilder();
        msg.append(e.getMessage());
        for(Throwable throwable : e.getSuppressed()){
            msg.append(throwable.getMessage());
        }
        return msg.toString();
   }
}
