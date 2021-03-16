package com.peter.search.config;

import com.peter.search.mq.DeltaDataImportConsumer;
import com.peter.search.mq.MQConsumerFactory;
import com.peter.search.util.PropertyUtils;
import io.netty.util.internal.ConcurrentSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import static com.peter.search.util.Constant.*;

/**
 * 
 * @author lsh12724
 *
 */
@Service
public class RedisMQConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RedisMQConsumer.class);

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private DeltaDataImportConsumer deltaDataImport;

    @Autowired
    PropertyUtils properties;

    /**
     * 需要watch redis 值的变化
     * */
    private ConcurrentSet<String> copyMsgServiceTagSet = new ConcurrentSet<>();

    public ConcurrentSet<String> getCopyMsgServiceTagSet(){
        return copyMsgServiceTagSet;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);

        // copy msg listener
        container.addMessageListener((message, bytes) -> {

            try{
                String channel = new String(message.getChannel());
                String serviceTag = new String(message.getBody());
                if(StringUtils.equals(channel, COPY_MSG_SERVICE_TAG_ADD)){
                    copyMsgServiceTagSet.add(serviceTag);
                } else if(StringUtils.equals(channel, COPY_MSG_SERVICE_TAG_REMOVE)){
                    copyMsgServiceTagSet.remove(serviceTag);
                } else if(StringUtils.equals(channel, COPY_MSG_SERVICE_TAG_START)){
                    // 开始消费copy Msg
                    MQConsumerFactory.getInstance().getConsumer(MQ_CONSUMER_GROUP_NAME_COPY+"_"+serviceTag,
                            MQ_TOPIC_NAME_COPY+"_"+serviceTag).resume();
                } else if(StringUtils.equals(channel, COPY_MSG_SERVICE_TAG_STOP)){
                    //  暂停消费copy Msg
                    MQConsumerFactory.getInstance().getConsumer(MQ_CONSUMER_GROUP_NAME_COPY+"_"+serviceTag,
                            MQ_TOPIC_NAME_COPY+"_"+serviceTag).suspend();
                }
            }catch (Exception e){
                logger.error("redis消息订阅异常：{}", e);
            }
        }, new PatternTopic("copy_msg_service_tag.*"));

        // cache reload listener
        container.addMessageListener((message, bytes) -> {

            try{
                String channel = new String(message.getChannel());
                if(StringUtils.equals(channel, RELOAD_SERVICE_TAG)){
                    //  reload serviceTag from db when it was edit
                    properties.loadAllServiceTag();
                }
            }catch (Exception e){
                logger.error("redis消息订阅异常：{}", e);
            }
        }, new PatternTopic("cache_reload.*"));
        return container;
    }
}