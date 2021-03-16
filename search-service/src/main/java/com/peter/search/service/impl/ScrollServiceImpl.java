package com.peter.search.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.service.responsewrap.QueryRespWrapFactory;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ScrollServiceImpl {

    @Autowired
    SearchServiceImpl searchService;

    @Autowired
    ESClientFactory esClientFactory;

    public SearchResultDTO scrollFirst(String serviceTag, JSONObject paramsObj){
        return searchService.search(serviceTag, paramsObj, searchRequest -> searchRequest.scroll("1m"));
    }

    public SearchResultDTO scrollAfter(String serviceTag, String scrollId) throws IOException {
        SearchResultDTO searchResultDTO = new SearchResultDTO();
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(TimeValue.timeValueSeconds(30));
        RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);
        SearchResponse searchScrollResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);

        QueryRespWrapFactory.wrapQueryResponse(searchScrollResponse, searchResultDTO);
        searchResultDTO.setTotalNum(searchScrollResponse.getHits().totalHits);
        searchResultDTO.setReturnSize(searchResultDTO.getData().size());

        if(searchScrollResponse.getHits().getHits().length == 0){
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();
        } else {
            searchResultDTO.setScrollId(searchScrollResponse.getScrollId());
        }
        return searchResultDTO;
    }

}
