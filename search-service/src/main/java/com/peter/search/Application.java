package com.peter.search;

import com.peter.search.mq.MQConsumerFactory;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.newnoa.logger.config.Level;
import com.peter.search.util.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * spring boot启动类
 *
 * @author 王海涛
 */

@MapperScan("com.peter.search.dao.mapper")
@EnableSwagger2
@SpringBootApplication
@EnableEurekaClient
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@EnableApolloConfig
public class Application {
    /**
     * LOGGER
     */
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    @Autowired
    private RestTemplateBuilder builder;

    // 使用RestTemplateBuilder来实例化RestTemplate对象，spring默认已经注入了RestTemplateBuilder实例
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return builder.setConnectTimeout(10000).setReadTimeout(10000).build();
    }

    public static void main(String[] args) {

        com.newnoa.logger.Logger.setGlobalLevel(Level.ERROR);
        com.newnoa.logger.Logger.setConsoleWriter(false);
        logger.info("############# search-service 开始启动 #############");
        
        try {
            SpringApplication.run(Application.class, args);
            
            // 启动所有增量更新的mq consumer
            MQConsumerFactory.getInstance().getConsumer(Constant.MQ_CONSUMER_GROUP_NAME);
            
        } catch (Exception e) {
            logger.error("search-service 启动异常", e);
        } finally {
            logger.info("############# search-service 启动完成 #############");
        }
    }

}
