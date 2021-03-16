package com.peter.search.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.peter.search.dto.MSearchResultDTO;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.service.MSearchService;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.service.querybuilder.QueryBuilderFactory;
import com.peter.search.service.responsewrap.QueryRespWrapFactory;
import com.peter.search.util.PropertyUtils;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量查询service
 *
 * @author 七星
 * */
@Service
public class MultipleSearchServiceImpl implements MSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(MultipleSearchServiceImpl.class);

    @Autowired
    PropertyUtils properties;

    @Autowired
    ESClientFactory esClientFactory;

    @Autowired
    QueryBuilderFactory queryBuilderFactory;

    /**
     * 搜索入口
     * 
     * @param serviceTag 搜索业务标示
     * @param paramsArray 查询条件
     * @return 查询结果
     * */
    @Override
    public MSearchResultDTO mSearch(String serviceTag, JSONArray paramsArray){

        MSearchResultDTO mSearchResultDTO = new MSearchResultDTO(1);
        try{
            MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
            for(int i = 0; i < paramsArray.size(); i++){

                // 根据查询参数，构造ES query
                SearchRequest searchRequest = queryBuilderFactory.buildSearchRequest(serviceTag, paramsArray.getJSONObject(i));
                searchRequest.indices(properties.getIndexAlias(serviceTag));
                searchRequest.types(properties.getTypeName(serviceTag));
                multiSearchRequest.add(searchRequest);
            }

            // 构建ES client
            RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);
            // 调用ES查询数据
            MultiSearchResponse mSearchResponse = client.multiSearch(multiSearchRequest);

            List<SearchResultDTO> dataList = new ArrayList();
            mSearchResponse.forEach(item -> {

                // 结果包装
                SearchResultDTO searchResult = new SearchResultDTO(1);

                if(item.isFailure()){
                    searchResult.setStatus(0);
                    searchResult.setErrMsg(item.getFailureMessage());
                    logger.warn("{}--搜索异常：{}", serviceTag, item.getFailureMessage());
                } else {
                    try{
                        QueryRespWrapFactory.wrapQueryResponse(item.getResponse(), searchResult);
                        searchResult.setTotalNum(item.getResponse().getHits().totalHits);
                        searchResult.setReturnSize(searchResult.getData().size());
                    }catch (Exception e){
                        searchResult.setStatus(0);
                        searchResult.setErrMsg(e.getMessage());
                        logger.error("{}--搜索异常：", serviceTag, e);
                    }
                }
                dataList.add(searchResult);
            });
            mSearchResultDTO.setData(dataList);
        }catch(Exception e){
            mSearchResultDTO.setStatus(SearchResultDTO.STATUS_FAILED);
            mSearchResultDTO.setErrMsg(serviceTag + "--搜索异常："+e.getMessage());
            logger.error("{}--批量搜索异常：", serviceTag, e);
        }
        return mSearchResultDTO;
   }
}
