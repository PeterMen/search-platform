package com.peter.search.annotation;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.common.message.Message;
import com.peter.search.dto.OP_TYPE;
import com.peter.search.pojo.DocData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SyncDataMQClient {

    private Integer timeout;

    private String address;

    private Integer maxMessageSize;

    private Integer retryTimes;

    private DefaultMQProducer producer;

    /**
     * 同步数据到ES对应的MQ
     * */
    public void syncData(String serviceTag, OP_TYPE opType, List<DocData> docDataList){
        Message msg = new Message();
        msg.setTopic(SyncDataAspect.TOPIC_NAME);
        msg.setTags(serviceTag);
        msg.setKeys(opType.toString());
        msg.setBody(JSON.toJSONString(docDataList).getBytes());
        try{
            // 保证消费时的顺序性
            getMQProducer().send(msg, new DefaultMessageQueueSelector(), serviceTag);
        }catch (Exception e){
            log.error("rocketMQ队列数据send失败", e);
        }
    }

    private DefaultMQProducer getMQProducer() {
        if(producer == null){
            synchronized (this){
                if(producer == null){
                    producer = createProducer();
                }
            }
        }
        return producer;
    }


    private DefaultMQProducer createProducer(){

        DefaultMQProducer producer = new DefaultMQProducer(SyncDataAspect.GROUP_NAME);
        producer.setNamesrvAddr(address);
        //如果需要同一个jvm中不同的producer往不同的mq集群发送消息，需要设置不同的instanceName
        //producer.setInstanceName(instanceName);
        if(maxMessageSize != null){
            producer.setMaxMessageSize(maxMessageSize);
        }
        if(timeout !=null){
            producer.setSendMsgTimeout(timeout);
        }
        //如果发送消息失败，设置重试次数，默认为2次
        if(retryTimes !=null ){
            producer.setRetryTimesWhenSendFailed(retryTimes);
        }
        try {
            producer.start();
            log.info(String.format("producer is start !!! groupName:[%s],namesrvAddr:[%s]"
                    , SyncDataAspect.GROUP_NAME, address));
        } catch (MQClientException e) {
            log.error(String.format("producer is error {}", e.getMessage(),e));
        }
        return producer;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setMaxMessageSize(Integer maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }
}
