package com.peter.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.dao.IndexAnalyzerDao;
import com.peter.search.dao.IndexDao;
import com.peter.search.dao.ServiceTagIndexDao;
import com.peter.search.entity.Index;
import com.peter.search.entity.IndexAnalyzer;
import com.peter.search.pojo.Column;
import com.peter.search.pojo.Field;
import com.peter.search.pojo.Setting;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.util.Constant;
import com.peter.search.pojo.DataType;
import com.peter.search.util.SettingUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.util.Assert;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 负责索引结构创建相关的服务
 * */
@Service
public class IndexCreateServiceImpl {


    private static final Logger logger = LoggerFactory.getLogger(IndexCreateServiceImpl.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IndexInfoServiceImpl indexInfo;
    @Autowired
    private IndexDao indexDao;
    @Autowired
    private ServiceTagIndexDao serviceTagIndexDao;
    @Autowired
    private IndexAnalyzerDao indexAnalyzerDao;
    @Autowired
    private ServiceTagInfoImpl serviceTagInfo;
    @Autowired
    private ESClientFactory esClientFactory;
    /**
     * 获取新索引名称
     * */
    public String generateIndexName(String serviceTag) {
        // 设置默认索引名称
        String indexName = serviceTagInfo.getIndexAlias(serviceTag);
        String date = LocalDate.now().toString().replaceAll("-","");
        String currentSeq = stringRedisTemplate.opsForValue().get(Constant.CURRENT_INDEX_SEQUENCE +serviceTag);
        if(StringUtils.isEmpty(currentSeq)){
            currentSeq = "1";
        } else {
            currentSeq = String.valueOf((Integer.valueOf(currentSeq)+1)%10);
        }
        stringRedisTemplate.opsForValue().set(Constant.CURRENT_INDEX_SEQUENCE + serviceTag, currentSeq);
        return indexName+"_"+date+"_"+currentSeq;
    }

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
     * 判断索引是否已存在
     * */
    public boolean indexExists(String serviceTag, String indexName) throws IOException {

        RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);
        GetIndexRequest request = new GetIndexRequest();
        request.indices(indexName);
        return client.indices().exists(request);
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
            return;
        } else if(oldIndexName == null){
            // 别名未被占用
            createIndexAlias(serviceTag, indexName, indexAlias);
        } else {
            // 别名切换（删除旧索引上的别名，添加别名到新的索引上）
            changeIndexAlias(serviceTag, oldIndexName, indexName, indexAlias);
        }
    }

    /**
     * 删除ES索引
     * */
    public boolean indexDelete(String serviceTag, String indexName){
        try {
            RestClient client = esClientFactory.getRestClient(serviceTag);
            Request request = new Request("DELETE", indexName);
            Response rs = client.performRequest(request);
            return rs.getStatusLine().getStatusCode() == 200;
        } catch (org.elasticsearch.client.ResponseException e){
            if( e.getResponse().getStatusLine().getStatusCode() == 404){
                return true;
            } else {
                logger.error("索引删除失败：", e);
                return false;
            }
        } catch (Exception e){
            logger.error("索引删除失败：", e);
            return false;
        }
    }

    /**
     * 根据现有索引copy一个新的出来
     * */
    public String createNewIndexCopy(String serviceTag)throws IOException{
        Index index = indexDao.selectOne(serviceTag);
        List<Column> columns = JSON.parseArray(index.getMapping(), Column.class);
        return createNewIndex(serviceTag, index.getShardingNum(), index.getReplicationNum(), columns);
    }


    /**
     * 创建ES上的索引，并将名称记录到DB（方便以后，根据serviceTag进行删除操作）
     * */
    @Transactional(rollbackFor=Exception.class)
    public String createNewIndex(String serviceTag, int shardNum, int replicaNum, List<Column> columns) throws IOException{
        Setting st = new Setting();
        st.setShardingNum(shardNum);
        st.setReplicationNum(replicaNum);
        String indexName = createSettingAndMapping(serviceTag, st, columns);
        serviceTagIndexDao.insert(serviceTag, indexName);
        return indexName;
    }

    /**
     * 生成新的索引名称，并创建setting和mapping
     * */
    private String createSettingAndMapping(String serviceTag, Setting setting, List<Column> columns) throws IOException {
        // 生成真实的索引名称
        String indexName = generateIndexName(serviceTag);

        // 如果生成的索引名称已存在，则无法创建，有可能是当天的序号使用完毕导致，需要手动处理
        Assert.isTrue(!indexExists(serviceTag, indexName), "索引重建序号使用完毕，请联系管理员处理。");

        // 查询用到的所有分析器
        List<IndexAnalyzer> analyzers = indexAnalyzerDao.getAllAnalyzer(serviceTag);
        setting.setUsedAnalyzers(getUsedAnalyzer(analyzers, columns));

        // 创建setting
        RestClient restClient = esClientFactory.getRestClient(serviceTag);
        Request settingRequest = new Request("PUT", indexName);
        settingRequest.setJsonEntity(setting.getSetting());
        restClient.performRequest(settingRequest);


        // column 转 field
        List<Field> fields = SettingUtil.transferToField(columns, Maps.uniqueIndex(analyzers.iterator(), IndexAnalyzer::getId));

        // 创建mapping
        Request mappingRequest = new Request("PUT", indexName+"/_mapping/"+ serviceTagInfo.getTypeName(serviceTag));
        mappingRequest.setJsonEntity(transferToMappings(fields).toJSONString());
        restClient.performRequest(mappingRequest);

        return indexName;
    }

    /**
     * 所有字段用到的analyzer
     * */
    private List<IndexAnalyzer> getUsedAnalyzer(List<IndexAnalyzer> analyzers, List<Column> columns){

        HashSet<Long> set = getAnalyzerIds(columns);
        return analyzers.stream().filter(r -> set.contains(r.getId())).collect(Collectors.toList());

    }

    /**
     * 获取所有字段用到的analyzer集合
     * */
    private HashSet<Long> getAnalyzerIds(List<Column> columns){
        HashSet<Long> set = new HashSet<>();
        for(Column column : columns){
            if(column.getAnalyzerId() != null) {
                set.add(column.getAnalyzerId());
            }
            if(column.getDataType() == DataType.OBJECT || column.getDataType() == DataType.NESTED){
                set.addAll(getAnalyzerIds(column.getColumns()));
            }
        }
        return set;
    }

    private JSONObject transferToMappings(List<Field> fields) {
        JSONObject mappingJson = new JSONObject();
        mappingJson.put("dynamic", false);
        mappingJson.put("properties", transferToProperties(fields));
        return mappingJson;
    }

    /**
     * 把字段设置转化成mapping
     * */
    private JSONObject transferToProperties(List<Field> fields) {

        // 编辑properties
        JSONObject propertiesJson = new JSONObject();
        fields.forEach(field -> {
            if(StringUtils.equals(field.getDataType().getKey(), DataType.TEXT_IK_SEARCH.getKey())){
                propertiesJson.put(field.getColumnName(),
                        ImmutableMap.of("type", "text","analyzer", field.getAnalyzer(),"search_analyzer", field.getSearchAnalyzer()));
            } else if(StringUtils.equals(field.getDataType().getKey(), DataType.ENGLISH.getKey())){
                propertiesJson.put(field.getColumnName(),
                        ImmutableMap.of("type", "text","analyzer", DataType.ENGLISH.getKey()));
            } else if(StringUtils.equals(field.getDataType().getKey(), DataType.TEXT_STANDARD_SEARCH.getKey())){
                propertiesJson.put(field.getColumnName(),
                        ImmutableMap.of("type", "text","analyzer", "standard"));
            } else if(StringUtils.equals(field.getDataType().getKey(), DataType.DATE.getKey())){
                propertiesJson.put(field.getColumnName(),
                        ImmutableMap.of("type", "date","format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd"));
            } else if(StringUtils.equals(field.getDataType().getKey(), DataType.OBJECT.getKey())){
                JSONObject objectJson = new JSONObject();
                objectJson.put("properties", transferToProperties(field.getFields()));
                propertiesJson.put(field.getColumnName(), objectJson);
            } else if(StringUtils.equals(field.getDataType().getKey(), DataType.NESTED.getKey())){
                JSONObject nestedJson = new JSONObject();
                nestedJson.put("type", "nested");
                nestedJson.put("properties", transferToProperties(field.getFields()));
                propertiesJson.put(field.getColumnName(), nestedJson);
            } else {
                if(field.getSearchAble() != null && field.getSearchAble()== false){
                    propertiesJson.put(field.getColumnName(),
                            ImmutableMap.of("type", field.getDataType().getKey(), "index", "false"));
                } else {
                    propertiesJson.put(field.getColumnName(), ImmutableMap.of("type", field.getDataType().getKey()));
                }
            }
        });
        return propertiesJson;
    }

    public void updateMapping(String serviceTag, List<Column> columns) throws IOException{
        RestClient restClient = esClientFactory.getRestClient(serviceTag);
        String indexName = indexInfo.getIndexNameByAlias(serviceTag, serviceTagInfo.getIndexAlias(serviceTag));

        // 查询用到的所有分析器
        List<IndexAnalyzer> analyzers = indexAnalyzerDao.getAllAnalyzer(serviceTag);
        // 创建mapping
        Request mappingRequest = new Request("PUT", indexName+"/_mapping/"+ serviceTagInfo.getTypeName(serviceTag));

        mappingRequest.setJsonEntity(transferToMappings(
                SettingUtil.transferToField(columns, Maps.uniqueIndex(analyzers.iterator(), IndexAnalyzer::getId))).toJSONString());
        restClient.performRequest(mappingRequest);
    }

    public void updateReplica(String serviceTag, int replicaNum) throws IOException{
        RestClient restClient = esClientFactory.getRestClient(serviceTag);
        String indexName = indexInfo.getIndexNameByAlias(serviceTag, serviceTagInfo.getIndexAlias(serviceTag));
        // 创建mapping
        Request mappingRequest = new Request("PUT", indexName+"/_settings");
        mappingRequest.setJsonEntity("{\"index\":{\"number_of_replicas\":"+replicaNum+"}}");
        restClient.performRequest(mappingRequest);
    }
}
