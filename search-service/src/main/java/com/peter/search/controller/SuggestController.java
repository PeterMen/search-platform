package com.peter.search.controller;

import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.Result;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.dto.SuggestWordDTO;
import com.peter.search.entity.Constant;
import com.peter.search.mq.DeltaDataImportConsumer;
import com.peter.search.pojo.DocData;
import com.peter.search.pojo.SuggesterGeneralRecord;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.service.impl.ScrollServiceImpl;
import com.peter.search.service.querybuilder.QueryBuilderFactory;
import com.peter.search.util.PropertyUtils;
import com.google.common.base.Joiner;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Api(tags = "联想词管理")
@RestController(value = "suggest")
@RequestMapping("/suggest")
public class SuggestController {

    private static final Logger logger = LoggerFactory.getLogger(SuggestController.class);
    public static final String ORIGINAL_WORD = "originalWord";
    public static final String TOKENIZED_WORD = "tokenizedWord";
    public static final String REGEX = "[.]";
    private static SuggesterGeneralRecord record = new SuggesterGeneralRecord();

    @Autowired
    private ScrollServiceImpl scrollService;

    @Autowired
    private DeltaDataImportConsumer deltaDataImport;

    @Autowired
    private ESClientFactory esClientFactory;

    @Autowired
    private PropertyUtils properties;

    @Autowired
    private QueryBuilderFactory queryBuilderFactory;

    @ApiOperation(value = "训练提示词", notes = "训练提示词", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/generalSuggestWord")
    public Result generalSuggestWord(@RequestBody SuggestWordDTO suggestWordDTO){

        // step1: 查询提示词来源字段
        Assert.notEmpty(suggestWordDTO.getSuggestFields(), "提示词字段不能为空");

        record.reset();
        new Thread(() -> {
            try {

                // 生成本次提示词的版本号
                Long batchVersion = System.currentTimeMillis();

                // step2 : 写提示词数据
                writeSuggesterData(suggestWordDTO, batchVersion);

                // step3: 删除旧版本数据
                deleteOldBatchData(suggestWordDTO.getServiceTag(), batchVersion);

                // step4 : 去重,保留重复值中的第一个
                distinctSuggester(suggestWordDTO.getConditionFields());
            }catch (IOException e){
                logger.error("提示词生成失败", e);
                record.addMessage("提示词生成失败:"+e.getMessage());
            } finally {
                record.setFinished();
            }
            record.addMessage("提示词训练完成。");
        }).start();

        record.addMessage("提示词生成中...");
        return Result.buildSuccess(record);
    }

    @ApiOperation(value = "查看训练词生成结果", notes = "查看训练词生成结果", produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/getSuggestResult")
    public Result getSuggestResult() {
        return Result.buildSuccess(record);
    }

    private void writeSuggesterData(SuggestWordDTO suggestWordDTO, Long batchVersion) throws IOException {

        String serviceTag = suggestWordDTO.getServiceTag();

        JSONObject queryParam = new JSONObject();
        queryParam.put("iRowSize", 100);
        queryParam.put("sFLParam", Joiner.on(",").join(suggestWordDTO.getSuggestFields(), suggestWordDTO.getConditionFields()));
        SearchResultDTO searchResultDTO = scrollService.scrollFirst(serviceTag, queryParam);

        do {
            // 生成doc data
            List<DocData> docDataList = new ArrayList<>();
            searchResultDTO.getData().forEach(data -> {
                JSONObject docDataJson = new JSONObject();
                docDataJson.put("type", 1);// 1:from 原商品数据，清洗得来   2：手动添加维护
                docDataJson.put("batchVersion", batchVersion);
                docDataJson.put("serviceTag", serviceTag);
                suggestWordDTO.getConditionFields().forEach(conditionField -> {
                    String[] deepField = conditionField.split(REGEX);
                    Object content  = getDeepFieldContent(data, deepField, deepField.length);
                    docDataJson.put(conditionField, content);
                });
                suggestWordDTO.getSuggestFields().forEach(suggestField -> {
                    JSONObject newDocDataJson = (JSONObject)docDataJson.clone();
                    String[] deepField = suggestField.split(REGEX);
                    Object content  = getDeepFieldContent(data, deepField, deepField.length);
                    newDocDataJson.put(ORIGINAL_WORD, content);
                    newDocDataJson.put(TOKENIZED_WORD, content);
                    DocData docData = new DocData();
                    docData.setDocData(newDocDataJson.toJSONString());
                    docData.setDocDataType("json");
                    docDataList.add(docData);
                });

            });

            // step2 : 写suggest数据
            deltaDataImport.indexInsert(
                    Constant.SERVICE_TAG_SUGGEST,
                    docDataList,
                    new DeltaDataImportConsumer.DeltaESRequestHandler(){
                        @Override
                        public void failed(Integer failedIndex, String errMsg){
                            logger.error("联想词生成失败：{}", errMsg);
                            record.addMessage("联想词生成失败：" + errMsg);
                        }
            });

            // next batch data
            searchResultDTO = scrollService.scrollAfter(serviceTag, searchResultDTO.getScrollId());
        } while (searchResultDTO.getScrollId() != null);
    }

    private void deleteOldBatchData(String serviceTag, Long batchVersion) {
        try{

            JSONObject deleteJson = new JSONObject();
            deleteJson.put("batchVersion", "!:"+batchVersion);
            deleteJson.put("type", 1);
            deleteJson.put("serviceTag", serviceTag);
            // 构建ES client
            RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);
            DeleteByQueryRequest deleteByQueryRequest = queryBuilderFactory.buildDeleteRequest(Constant.SERVICE_TAG_SUGGEST, deleteJson);
            deleteByQueryRequest.indices(properties.getIndexAlias(Constant.SERVICE_TAG_SUGGEST));
            deleteByQueryRequest.types(properties.getTypeName(Constant.SERVICE_TAG_SUGGEST));
            client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
        }catch (Exception e){
            logger.error("deleteByQuery异常：", e);
            record.addMessage("deleteByQuery异常：:" + e.getMessage());
        }
    }

    private void distinctSuggester(List<String> conditionFields) throws IOException {
        JSONObject distinct = new JSONObject();
        distinct.put("iRowSize", 500);
        distinct.put("sFLParam", ORIGINAL_WORD);
        if(CollectionUtils.isEmpty(conditionFields)){
            distinct.put("sSort", "originalWord asc");
        } else {
            distinct.put("sSort", StringUtils.join(conditionFields, " asc,") + " asc, originalWord asc");
        }
        SearchResultDTO distinctResultDTO = scrollService.scrollFirst(Constant.SERVICE_TAG_SUGGEST, distinct);
        String currentWord = "";
        do{
            // 生成doc data
            List<DocData> deleteDocList = new ArrayList<>();
            for(Map data : distinctResultDTO.getData()){
                String target = Objects.toString(data.get(ORIGINAL_WORD), "");
                if(StringUtils.isEmpty(target) || StringUtils.equals(currentWord, target)){
                    DocData docData = new DocData();
                    docData.setDocId(String.valueOf(data.get("docId")));
                    deleteDocList.add(docData);
                } else {
                    currentWord = Objects.toString(data.get(ORIGINAL_WORD), "");
                }
            }
            deltaDataImport.indexDelete(Constant.SERVICE_TAG_SUGGEST, deleteDocList, new DeltaDataImportConsumer.DeltaESRequestHandler(){
                @Override
                public void failed(Integer failedIndex, String errMsg){
                    logger.error("删除重复联想词失败：{}", errMsg);
                    record.addMessage("删除重复联想词失败：" + errMsg);
                }
            });
            // next batch data
            distinctResultDTO = scrollService.scrollAfter(Constant.SERVICE_TAG_SUGGEST, distinctResultDTO.getScrollId());
        } while (distinctResultDTO.getScrollId() != null);
    }

    Object getDeepFieldContent(Object data, String[] deepField, int length) {
        if (data == null) return null;
        else if (data instanceof Map && length != 1) {
            return getDeepFieldContent(((Map) data).get(deepField[deepField.length - length]), deepField, length - 1);
        }
        else if (data instanceof List && length != 1) {
            for (Object subData : (List) data) {
                // TODO 如果是数组，应该返回多个值
                return getDeepFieldContent(subData, deepField, length);
            }
        }
        else if (data instanceof Map && length == 1) return ((Map) data).get(deepField[deepField.length - 1]);
        else if (data instanceof List && length == 1 && !((List) data).isEmpty()) return ((List) data).get(0);
        return data;
    }
}
