package com.peter.search.controller;

import com.alibaba.fastjson.JSON;
import com.peter.search.dao.DBMsgDao;
import com.peter.search.dto.OP_TYPE;
import com.peter.search.entity.FailedMsg;
import com.peter.search.mq.DeltaDataImportConsumer;
import com.peter.search.pojo.DocData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@DependsOn("webAppContextUtil")
public class FailedMsgConsumer implements InitializingBean {

    public static final String MSG = "异常消息重试失败：";
    @Autowired
    private DeltaDataImportConsumer deltaDataImport;

    @Autowired
    DBMsgDao dbMsgDao;

    @Override
    public void afterPropertiesSet(){
        Thread t = new Thread(() ->{
            while (true){
                try {
                    consumeFailedMsg();
                }catch (Exception e){
                    log.error(MSG, e);
                    try {
                        // sleep 3分钟
                        Thread.sleep(180000);
                    } catch (InterruptedException e2) {
                        log.error(MSG, e2);
                    }
                }
            }
        });
        t.setName("failed-msg-consumer");
        t.setDaemon(true);
        t.start();
    }

    private void consumeFailedMsg(){

        List<FailedMsg> failedMsgs = dbMsgDao.getFailedMsgList();
        if(CollectionUtils.isEmpty(failedMsgs)){
            try {
                Thread.sleep(3000);
                return;
            } catch (InterruptedException e) {
                log.error("异常消息重试失败：", e);
            }
        }
        failedMsgs.forEach(failedMsg -> {

            // set handler
            ESRequestHandlerFromDB handler = new ESRequestHandlerFromDB();
            handler.setMsgId(failedMsg.getId());
            handler.setDbMsgDao(dbMsgDao);

            String serviceTag = failedMsg.getServiceTag();
            String opType = failedMsg.getOpType();
            List<DocData> docDataList = Arrays.asList(JSON.parseObject(failedMsg.getMsgContent(), DocData.class));
            if(StringUtils.equals(opType, OP_TYPE.DELETE.name())){
                // 删除
                deltaDataImport.indexDelete(serviceTag, docDataList, handler);
            } else if(StringUtils.equals(opType, OP_TYPE.INSERT.name())) {
                // insert or 全量更新
                deltaDataImport.indexInsert(serviceTag, docDataList, handler);
            } else if(StringUtils.equals(opType, OP_TYPE.UPSERT.name())) {
                // partial update
                deltaDataImport.indexUpdate(serviceTag, docDataList, true, handler);
            } else if(StringUtils.equals(opType, OP_TYPE.UPDATE.name())) {
                // partial update
                deltaDataImport.indexUpdate(serviceTag, docDataList, false, handler);
            }
        });
    }

    /**
     * es请求失败后的处理逻辑
     * */
    @Data
    public class ESRequestHandlerFromDB extends DeltaDataImportConsumer.DeltaESRequestHandler {

        private DBMsgDao dbMsgDao;
        private Long msgId;

        @Override
        public void failed(Integer failedIndex, String errMsg){

            try {
                // 记录metrics
                dataMetricsService.metricsLog(serviceTag, errMsg);

                // 更新失败消息
                dbMsgDao.msgFailedAgain(msgId, errMsg);
            } catch (Exception e){
                log.error("异常处理失败：", e);
            }
        }

        @Override
        public void success(Integer successCount){
            dbMsgDao.deleteSuccessMsg(msgId);
            dataMetricsService.addSuccessCount(serviceTag, successCount);
            dataMetricsService.minusFailedCount(serviceTag, successCount);
        }
    }
}
