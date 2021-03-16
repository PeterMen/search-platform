package com.peter.search;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.newnoa.logger.config.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * spring boot启动类
 *
 * @author 王海涛
 */

@MapperScan("com.peter.search.dao.mapper")
@EnableSwagger2
@SpringBootApplication(scanBasePackages = {"com.peter.search"})
@EnableFeignClients({ "com.peter.search.api"})
@EnableEurekaClient
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@EnableApolloConfig
public class Application {
    /**
     * LOGGER
     */
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        com.newnoa.logger.Logger.setGlobalLevel(Level.INFO);
        com.newnoa.logger.Logger.setConsoleWriter(false);
        logger.info("############# search-manager 开始启动 #############");
        
        try {
            SpringApplication.run(Application.class, args);
        } catch (Exception e) {
            logger.error("search-manager 启动异常", e);
        } finally {
            logger.info("############# search-manager 启动完成 #############");
        }
    }

}
