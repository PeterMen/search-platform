package com.peter.search.service.impl;

import com.peter.search.service.client.ESClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 负责索引相关信息查询
 * */
@Service
public class IndexInfoServiceImpl {

    @Autowired
    private ESClientFactory esClientFactory;

    /**
     * 通过别名获取索引的真实名称
     * */
    public String getIndexNameByAlias(String serviceTag, String indexAlias) throws IOException{
        try{
            RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);
            GetIndexRequest getIndexRequest = new GetIndexRequest().indices(indexAlias);
            GetIndexResponse getIndexResponse = client.indices().get(getIndexRequest, RequestOptions.DEFAULT);
            return getIndexResponse.getIndices()[0];
        }catch (ElasticsearchStatusException statusException){
            if(StringUtils.equals("NOT_FOUND", statusException.status().name())){
                return null;
            }
        }
        return null;
    }
}
