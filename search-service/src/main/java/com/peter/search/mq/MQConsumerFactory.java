package com.peter.search.mq;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.peter.search.util.WebAppContextUtil;
import com.peter.search.util.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MQConsumerFactory {
    private static final Logger logger = LoggerFactory.getLogger(MQConsumerFactory.class);

    private static volatile MQConsumerFactory instance;

    private static Map<String, DefaultMQPushConsumer> consumerMap = new HashMap(16);

    public static MQConsumerFactory getInstance(){
        if(instance == null){
            synchronized (MQConsumerFactory.class){
                if(instance == null){
                    instance = new MQConsumerFactory();
                }
            }
        }
        return instance;
    }

    public void addConsumer(String groupName, DefaultMQPushConsumer consumer){
        consumerMap.put(groupName, consumer);
    }

    public DefaultMQPushConsumer getConsumer(String groupName){

        if(consumerMap.get(groupName) == null){
            synchronized (consumerMap){
                if(consumerMap.get(groupName) == null){
                    addConsumer(groupName, createMQConsumer(groupName, Constant.MQ_TOPIC_NAME));
                }
            }
        }
        return consumerMap.get(groupName);
    }

    public DefaultMQPushConsumer getConsumer(String groupName, String topicName){

        if(consumerMap.get(groupName) == null){
            synchronized (consumerMap){
                if(consumerMap.get(groupName) == null){
                    addConsumer(groupName, createMQConsumer(groupName, topicName));
                }
            }
        }
        return consumerMap.get(groupName);
    }

    private DefaultMQPushConsumer createMQConsumer(String groupName, String topicName){

        MQConfigurations mqConfigurations = (MQConfigurations) WebAppContextUtil.getBean("MQConfigurations");
        MQConsumerListener mqConsumerListener = (MQConsumerListener)WebAppContextUtil.getBean("MQConsumerListener");
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(groupName);
        consumer.setNamesrvAddr(mqConfigurations.getNamesrvAddr());
        consumer.setConsumeThreadMin(mqConfigurations.getConsumeThreadMin());
        consumer.setConsumeThreadMax(mqConfigurations.getConsumeThreadMax());
        consumer.registerMessageListener(mqConsumerListener);
        /**
         * ??????Consumer???????????????????????????????????????????????????????????????????????????
         * ????????????????????????????????????????????????????????????????????????
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        /**
         * ?????????????????????????????????????????????????????????
         */
        //consumer.setMessageModel(MessageModel.CLUSTERING);
        /**
         * ?????????????????????????????????????????????1???
         */
        consumer.setConsumeMessageBatchMaxSize(mqConfigurations.getConsumeMessageBatchMaxSize());
        try {
            consumer.subscribe(topicName, "*");
            consumer.start();
            logger.info("consumer is start !!! groupName:{},namesrvAddr:{}",groupName,mqConfigurations.getNamesrvAddr());
        }catch (MQClientException e){
            logger.error("consumer is start !!! groupName:{},namesrvAddr:{}",groupName,mqConfigurations.getNamesrvAddr(),e);
        }
        return consumer;
    }

}
