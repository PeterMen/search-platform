package com.peter.search.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.peter.search.dao.DBMsgDao;
import com.peter.search.datametrics.DataMetricFactory;
import com.peter.search.dto.OP_TYPE;
import com.peter.search.entity.FailedMsg;
import com.peter.search.pojo.DocData;
import com.peter.search.config.RedisMQConsumer;
import com.peter.search.util.Constant;
import lombok.Data;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component(value = "MQConsumerListener")
public class MQConsumerListener implements MessageListenerConcurrently {

    private static final Logger logger = LoggerFactory.getLogger(MQConsumerListener.class);

    @Autowired
    private DBMsgDao dbMsgDao;

    @Autowired
    private DeltaDataImportConsumer deltaDataImport;

    @Autowired
    private RedisMQConsumer redisMQConsumer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     *  默认msgs里只有一条消息，可以通过设置consumeMessageBatchMaxSize参数来批量接收消息<br/>
     *  不要抛异常，如果没有return CONSUME_SUCCESS ，consumer会重新消费该消息，直到return CONSUME_SUCCESS
     */
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        if(CollectionUtils.isEmpty(msgs)){
            logger.info("接受到的消息为空，不处理，直接返回成功");
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
        MessageExt messageExt = msgs.get(0);
        logger.debug("接受到的消息为：{}", messageExt.toString());

        String serviceTag = messageExt.getTags();
        String opType = messageExt.getKeys();

        // copy msg
        if(redisMQConsumer.getCopyMsgServiceTagSet().contains(serviceTag)){
            Message msg = new Message();
            msg.setTopic(Constant.MQ_TOPIC_NAME_COPY+"_"+serviceTag);
            msg.setTags(serviceTag);
            msg.setKeys(opType);
            msg.setBody(messageExt.getBody());
            try{
                // 不能用同一个生产者，也不能用同一个消费者
                MQProducerFactory.getInstance().getProducer(Constant.MQ_PRODUCER_GROUP_NAME_COPY+"_"+serviceTag).send(msg);
            }catch (Exception e){
                logger.error("rocketMQ队列数据send失败", e);
            }
        }

        //  判断该消息是否重复消费（RocketMQ不保证消息不重复，如果你的业务需要保证严格的不重复消息，需要你自己在业务端去重）
        // 消息消费失败，不要重试
        int reconsume = messageExt.getReconsumeTimes();
        if(reconsume > 0 || StringUtils.isEmpty(serviceTag)){
            //消息已经重试了3次，如果不需要再次消费，则返回成功
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }

        // create es request handler
        ESRequestHandlerFromMQ handler = new ESRequestHandlerFromMQ();
        handler.setDbMsgDao(dbMsgDao);
        handler.setOpType(opType);

        List<DocData> docDataList = JSON.parseArray(new String(messageExt.getBody())).toJavaList(DocData.class);

        // 对doc去重，防止version conflict问题
        docDataList = distinctDocDataList(serviceTag, opType, docDataList);

        DataMetricFactory.getInstance().getDeltaDataMetrics(serviceTag).addAcceptCount(docDataList.size());
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

        // 如果没有return success ，consumer会重新消费该消息，直到return success
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private List<DocData> distinctDocDataList(String serviceTag, String opType, List<DocData> docDataList) {

        // 去重1:同一个消息内的重复数据去重，根据docId distinct
        HashSet<String> dd = new HashSet();
        docDataList = docDataList.stream().filter(r->dd.add(r.getDocId())).collect(Collectors.toList());
        logger.info("接受到的消息docId为：{},操作类型为：{}，serviceTag:{}", dd.toString(), opType, serviceTag);

        // 去重2：不同消息之间的并发数据去重，间隔为2s，就是说2s内如果有重复数据，则延后另外一个
        for(DocData r : docDataList){
            String redisKey = serviceTag+opType+r.getDocId();
            String exist = stringRedisTemplate.opsForValue().get(redisKey);
            if(exist == null){
                stringRedisTemplate.opsForValue().set(redisKey, redisKey, 2, TimeUnit.SECONDS);
            } else {
                try {
                    // 延时2s消费该消息
                    Thread.sleep(RandomUtils.nextInt(100, 2000));
                    break;
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return docDataList;
    }

    /**
     * es请求失败后的处理逻辑
     * */
    @Data
    public class ESRequestHandlerFromMQ extends DeltaDataImportConsumer.DeltaESRequestHandler {

        private DBMsgDao dbMsgDao;
        private String opType;

        public void failed(Integer failedIndex, String errMsg){

            try{
                // 记录metrics
                dataMetricsService.metricsLog(serviceTag, errMsg);
                dataMetricsService.addFailedCount(serviceTag, 1);

                // 保存失败消息
                FailedMsg failedMsg = new FailedMsg();
                failedMsg.setServiceTag(serviceTag);
                failedMsg.setOpType(opType);
                failedMsg.setMsgContent(JSON.toJSONString(docDataList.get(failedIndex)));
                failedMsg.setFailedReason(errMsg);
                dbMsgDao.saveFailedMsg(failedMsg);
            }catch (Exception e){
                logger.error("异常数据保存异常：", e);
            }
        }
    }
}
