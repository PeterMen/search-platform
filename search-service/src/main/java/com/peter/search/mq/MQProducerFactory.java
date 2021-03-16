package com.peter.search.mq;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.peter.search.util.WebAppContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MQProducerFactory {

    private static final Logger logger = LoggerFactory.getLogger(MQProducerFactory.class);
    private static MQProducerFactory instance;

    private static Map<String, DefaultMQProducer> producerMap = new HashMap(16);

    public static MQProducerFactory getInstance(){
        if(instance == null){
            synchronized (MQProducerFactory.class){
                if(instance == null){
                    instance = new MQProducerFactory();
                }
            }
        }
        return instance;
    }

    public void addProducer(String groupName, DefaultMQProducer producer){
        producerMap.put(groupName, producer);
    }

    public DefaultMQProducer getProducer(String groupName){
        if(producerMap.get(groupName) == null){
            synchronized (producerMap){
                if(producerMap.get(groupName) == null){
                    addProducer(groupName, createMQProducer(groupName));
                }
            }
        }
        return producerMap.get(groupName);
    }

    private DefaultMQProducer createMQProducer(String groupName){

        DefaultMQProducer producer = new DefaultMQProducer(groupName);

        MQConfigurations mqConfigurations = (MQConfigurations) WebAppContextUtil.getBean("MQConfigurations");
        producer.setNamesrvAddr(mqConfigurations.getNamesrvAddr());
        //如果需要同一个jvm中不同的producer往不同的mq集群发送消息，需要设置不同的instanceName
        //producer.setInstanceName(instanceName);
        if(mqConfigurations.getMaxMessageSize()!=null){
            producer.setMaxMessageSize(mqConfigurations.getMaxMessageSize());
        }
        if(mqConfigurations.getSendMsgTimeout() !=null){
            producer.setSendMsgTimeout(mqConfigurations.getSendMsgTimeout());
        }
        //如果发送消息失败，设置重试次数，默认为2次
        if(mqConfigurations.getRetryTimesWhenSendFailed() !=null ){
            producer.setRetryTimesWhenSendFailed(mqConfigurations.getRetryTimesWhenSendFailed() );
        }

        try {
            producer.start();

            logger.info("producer is start !!! groupName:{},namesrvAddr:{}"
                    , groupName, mqConfigurations.getNamesrvAddr());
        } catch (MQClientException e) {
            logger.error("producer is error {}", e);
        }
        return producer;
    }

}
