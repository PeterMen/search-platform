package com.peter.search.config;

import com.peter.search.annotation.SyncDataMQClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SyncDataMQClientConfig {

    @Value("${rocketmq.producer.sendMsgTimeout}")
    private Integer timeout;

    @Value("${rocketmq.producer.namesrvAddr}")
    private String address;

    @Value("${rocketmq.producer.maxMessageSize}")
    private Integer maxMessageSize;

    @Value("${rocketmq.producer.retryTimesWhenSendFailed}")
    private Integer retryTimes;

    @Bean
    public SyncDataMQClient syncDataMQClient(){
        SyncDataMQClient client = new SyncDataMQClient();
        client.setAddress(address);
        client.setMaxMessageSize(maxMessageSize);
        client.setRetryTimes(retryTimes);
        client.setTimeout(timeout);
        return client;
    }
}

