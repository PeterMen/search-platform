package com.peter.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.util.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
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
    ESClientFactory esClientFactory;

    @Autowired
    PropertyUtils properties;

    /**
     * 获取mappings
     * */
    public JSONObject getMappingProperties(String serviceTag, String indexName) throws IOException {

        // 获取mappings
        RestClient restClient = esClientFactory.getRestClient(serviceTag);
        Response rs = restClient.performRequest("GET", indexName);
        JSONObject rsJson2 = JSON.parseObject(EntityUtils.toString(rs.getEntity()));
        JSONObject indexJson = rsJson2.getJSONObject(indexName);
        String mappingsStr = indexJson.getString("mappings");
        String typeName = properties.getTypeName(serviceTag);
        return JSON.parseObject(mappingsStr).getJSONObject(typeName).getJSONObject("properties");
    }

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
