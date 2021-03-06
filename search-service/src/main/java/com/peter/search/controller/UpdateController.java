package com.peter.search.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.client.producer.SendStatus;
import com.alibaba.rocketmq.common.message.Message;
import com.peter.search.annotation.DefaultMessageQueueSelector;
import com.peter.search.api.UpdateServiceApi;
import com.peter.search.dao.DBMsgDao;
import com.peter.search.datametrics.DataMetricFactory;
import com.peter.search.dto.OP_TYPE;
import com.peter.search.dto.Result;
import com.peter.search.dto.UpdateRequestDTO;
import com.peter.search.entity.FailedMsg;
import com.peter.search.mq.MQProducerFactory;
import com.peter.search.pojo.DocData;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.service.impl.IndexWriterServiceImpl;
import com.peter.search.service.querybuilder.QueryBuilderFactory;
import com.peter.search.util.PropertyUtils;
import com.google.common.collect.Lists;
import com.peter.search.util.Constant;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  ??????
 *
 * @author ??????
 * @date 2018???02???02???
 */
@Api(tags = "????????????????????????", position=9)
@RestController(value = "esUpdateService")
public class UpdateController implements UpdateServiceApi {

    private static final Logger logger = LoggerFactory.getLogger(UpdateController.class);
    private final String ENCODE_UTF8 = "UTF-8";

    @Autowired
    ESClientFactory esClientFactory;

    @Autowired
    PropertyUtils properties;

    @Autowired
    IndexWriterServiceImpl indexWriter;

    @Autowired
    DBMsgDao dbMsgDao;

    @Autowired
    QueryBuilderFactory queryBuilderFactory;

    /**
     * ES????????????
     *
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "??????DSL????????????", notes = "??????DSL????????????",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceTag", value = "???????????????APP???H5???PC", required = true, dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "updateDSLJson", value = "es dsl????????????", required = true, dataType = "string", paramType = "form")
    })
    @Override
    public String restfulUpdate(@RequestParam(name = "serviceTag") String serviceTag,
                                @RequestParam(name = "docId") String docId,
                                @RequestParam(name = "updateDSLJson") String updateDSLJson) throws Exception{

        try {
            String index = properties.getIndexAlias(serviceTag);
            String type = properties.getTypeName(serviceTag);
            StringEntity stringEntity = new StringEntity(updateDSLJson,ENCODE_UTF8);
            stringEntity.setContentEncoding(ENCODE_UTF8);
            stringEntity.setContentType("application/json");
            Response response = esClientFactory.getRestClient(serviceTag)
                    .performRequest("POST", index + "/" + type + "/" + docId + "/_update?retry_on_conflict=5", new HashMap<>(4), stringEntity);
            return EntityUtils.toString(response.getEntity());
        } catch (ResponseException e) {
            logger.error("rest update  error {}",updateDSLJson, e);
            throw new Exception(e.getMessage());
        }catch (Exception e){
            logger.error("rest update  error", e);
            throw e;
        }
    }

    @ApiOperation(value = "??????????????????", notes = "??????????????????",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public String updateByScript(@RequestParam(name = "serviceTag") String serviceTag,
                                 @RequestParam(name = "docId") String docId,
                                 @RequestParam(name = "paramMap") Map<String, Object> paramMap,
                                 @RequestParam(name = "scriptId") String scriptId) throws Exception {
        String index = properties.getIndexAlias(serviceTag);
        String type = properties.getTypeName(serviceTag);
        RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);
        Script script = new Script(ScriptType.STORED, null, scriptId, paramMap);

        UpdateRequest updateRequest = new UpdateRequest(index, type, docId).retryOnConflict(5).script(script);
        try {
            UpdateResponse response = client.update(updateRequest);
            return response.status().name();
        } catch (ResponseException e) {
            logger.error("script update  error {}", paramMap, e);
            throw new Exception(e.getMessage());
        } catch (Exception e) {
            logger.error("script update  error", e);
            throw e;
        }
    }

    /**
     * ES????????????
     *
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "??????docId????????????", notes = "??????docId????????????",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Override
    public Result deleteByDocId(@RequestParam(name = "serviceTag") String serviceTag,
                                @RequestParam(name = "docId") String docId) {

        logger.info("??????????????????serviceTag:"+serviceTag +",docId:"+docId);
        DocData docData = new DocData();
        docData.setDocId(docId);
        indexWriter.indexDelete(serviceTag, Lists.newArrayList(docData), new IndexWriterServiceImpl.ESRequestHandler() {
            @Override
            public void failed(Integer failedIndex, String errMsg) {
                // do nothing
            }
            @Override
            public void success(Integer successCount) {
                // do nothing
            }
        });
        return Result.buildSuccess();
    }

    /**
     * ES????????????
     *
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "??????????????????????????????", notes = "??????????????????????????????",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    @ResponseBody
    public Result deleteByQuery(@RequestParam(name = "serviceTag") String serviceTag,
                                @RequestParam(name = "deleteJson") String deleteJson){

        logger.info("??????????????????serviceTag:"+serviceTag +",deleteJson:"+deleteJson);
        try{
            // ??????ES client
            RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);
            DeleteByQueryRequest deleteByQueryRequest = queryBuilderFactory.buildDeleteRequest(serviceTag, JSON.parseObject(deleteJson));
            deleteByQueryRequest.indices(properties.getIndexAlias(serviceTag));
            deleteByQueryRequest.types(properties.getTypeName(serviceTag));
            client.deleteByQueryAsync(deleteByQueryRequest, RequestOptions.DEFAULT, new ActionListener<BulkByScrollResponse>() {
                @Override
                public void onResponse(BulkByScrollResponse bulkByScrollResponse) {
                    DataMetricFactory.getInstance()
                            .getDeltaDataMetrics(serviceTag)
                            .addLog("deleteByQuery??????????????????"+JSON.toJSONString(bulkByScrollResponse));
                }

                @Override
                public void onFailure(Exception e) {
                    DataMetricFactory.getInstance()
                            .getDeltaDataMetrics(serviceTag)
                            .addLog("deleteByQuery??????????????????"+e.getMessage());
                }
            });
        }catch (Exception e){
            logger.error("deleteByQuery?????????", e);
            return Result.buildErr(e.getMessage());
        }
        return Result.buildSuccess();
    }

    /**
     * ??????????????????????????????
     * */
    @Override
    @ResponseBody
    public Result deltaImportData(@RequestBody UpdateRequestDTO updateRequestDTO){

        String serviceTag = updateRequestDTO.getServiceTag();
        OP_TYPE opType = updateRequestDTO.getOpType();
        List<DocData> docDataList = updateRequestDTO.getDocDataList();

        if(StringUtils.isEmpty(serviceTag)){
            return Result.buildErr("serviceTag????????????");
        } else if(CollectionUtils.isEmpty(docDataList)){
            return Result.buildErr("docDataList????????????");
        } else if (opType == null){
            return Result.buildErr("opType????????????");
        }

        Message msg = new Message();
        msg.setTopic(Constant.MQ_TOPIC_NAME);
        msg.setTags(serviceTag);
        msg.setKeys(opType.name());
        msg.setBody(JSON.toJSONString(docDataList).getBytes());
        try{
            SendResult sendResult = MQProducerFactory.getInstance().getProducer(Constant.MQ_PRODUCER_GROUP_NAME)
                    .send(msg, new DefaultMessageQueueSelector(), serviceTag);
            if(SendStatus.SEND_OK != sendResult.getSendStatus()){
                sendMsgToDB(serviceTag, opType, docDataList, sendResult.getSendStatus().toString());
            }
        }catch (Exception e){
            logger.error("rocketMQ????????????send??????", e);
            sendMsgToDB(serviceTag, opType, docDataList, e.getMessage());
            return Result.buildErr(e.getMessage());
        }
        return Result.buildSuccess();
    }

    private void sendMsgToDB(String serviceTag, OP_TYPE opType, List<DocData> docDataList, String errMsg) {
        FailedMsg failedMsg = new FailedMsg();
        failedMsg.setServiceTag(serviceTag);
        failedMsg.setOpType(opType.name());
        failedMsg.setFailedReason("rocketMQ????????????send??????:" + errMsg);
        docDataList.forEach(docData -> {
            failedMsg.setMsgContent(JSON.toJSONString(docData));
            failedMsg.setId(null);
            dbMsgDao.saveFailedMsg(failedMsg);
        });
    }

}
