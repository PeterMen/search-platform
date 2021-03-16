package com.peter.search.service.impl;

import com.peter.search.datametrics.DataMetricFactory;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.util.PropertyUtils;
import com.peter.search.util.Constant;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 负责索引结构创建相关的服务
 * */
@Service
public class IndexCreateServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(IndexCreateServiceImpl.class);

    @Autowired
    private IndexInfoServiceImpl indexInfo;
    @Autowired
    private PropertyUtils properties;
    @Autowired
    private ESClientFactory esClientFactory;

    /**
     * 切换别名
     * */
    private void changeIndexAlias(String serviceTag, String oldIndexName, String newIndexName, String indexAlias) throws IOException{
        IndicesAliasesRequest request = new IndicesAliasesRequest();
        IndicesAliasesRequest.AliasActions removeAliasAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                        .index(oldIndexName)
                        .alias(indexAlias);
        IndicesAliasesRequest.AliasActions addAliasAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                        .index(newIndexName)
                        .alias(indexAlias);
        request.addAliasAction(addAliasAction);
        request.addAliasAction(removeAliasAction);
        esClientFactory.getHighLevelClient(serviceTag).indices().updateAliases(request, RequestOptions.DEFAULT);
    }

    /**
     * 创建别名
     * */
    private void createIndexAlias(String serviceTag, String indexName, String indexAlias) throws IOException{
        IndicesAliasesRequest request = new IndicesAliasesRequest();
        IndicesAliasesRequest.AliasActions addAliasAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                        .index(indexName)
                        .alias(indexAlias);
        request.addAliasAction(addAliasAction);
        esClientFactory.getHighLevelClient(serviceTag).indices().updateAliases(request, RequestOptions.DEFAULT);
    }

    /**
     * 索引可用性检查
     * // check3: totalCount 和 successCount，，， 由于successCount是异步回调的，所以会比真实es的数值慢，check3不能使用
     * //        if(Math.abs(totalCount-totalSuccessCount) > maxErrCount){
     * //            DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).addLog("文档数偏离值过大，总条数:"+ totalCount+"，但只处理了："+totalSuccessCount+"条");
     * //            return false;
     * //        }
     * */
    public boolean checkIndexAvailable(String serviceTag, String newIndexName) throws IOException{

        // check1: failedCount
        Integer maxErrCount = Integer.valueOf(properties.getProperty(serviceTag + Constant.MAX_ERR_DOC_COUNT, "10"));
        Integer failedCount = DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).getFailedCount().get();
        Integer totalCount = DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).getTotalCount();

        if(failedCount > maxErrCount){
            DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).addLog("错误文档数过多，failedCount:" + failedCount);
            return false;
        }

//        ES索引数据有延迟，导致数据条数与successCount不匹配
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(newIndexName);
        searchRequest.types(properties.getTypeName(serviceTag));
        RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);

        // 最大等待3分钟
        int maxWaitTimes = 30;
        long indexRealTotalCount = 0L;
        for(int i=0; i< maxWaitTimes; i++){

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            indexRealTotalCount = searchResponse.getHits().totalHits;
            if(Math.abs(indexRealTotalCount-totalCount) < maxErrCount){
                break;
            }
            try { Thread.sleep(6000); }catch (InterruptedException e){logger.error("InterruptedException", e);}
        }

        // check2: successCount 和写入ES真实的count数据
        if(Math.abs(indexRealTotalCount-totalCount) > maxErrCount){
            DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).addLog("文档数偏离值过大，文档总数："+ totalCount+"，但索引成功的文档数："+indexRealTotalCount);
            return false;
        }

        return true;
    }


    /**
     * 更新index.refresh_interval
     * */
    public void updateRefreshInterval(String serviceTag, String indexName, String refreshInterval) throws IOException {

        RestClient restClient = esClientFactory.getRestClient(serviceTag);
        Request mappingRequest = new Request("PUT", indexName+"/_settings");
        mappingRequest.setJsonEntity("{\"index.refresh_interval\":\""+refreshInterval+"\"}");
        restClient.performRequest(mappingRequest);
    }

    /**
     * 给索引创建别名
     * */
    public void createAliasForIndex(String serviceTag, String indexName, String indexAlias) throws IOException{

        // 检查该别名是否被其他索引占用
        String oldIndexName = indexInfo.getIndexNameByAlias(serviceTag, indexAlias);

        //如果该索引已经存在该别名，则不需要切换别名
        if(StringUtils.equals(indexName, oldIndexName)){
        } else if(oldIndexName == null){
            // 别名未被占用
            createIndexAlias(serviceTag, indexName, indexAlias);
        } else {
            // 别名切换（删除旧索引上的别名，添加别名到新的索引上）
            changeIndexAlias(serviceTag, oldIndexName, indexName, indexAlias);
        }
    }

}
