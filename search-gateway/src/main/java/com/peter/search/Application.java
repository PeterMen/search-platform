package com.peter.search;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.newnoa.logger.config.Level;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableApolloConfig
@EnableEurekaClient
@EnableZuulProxy
public class Application {

    public static void main(String[] args) {
        com.newnoa.logger.Logger.setGlobalLevel(Level.INFO);
        com.newnoa.logger.Logger.setConsoleWriter(false);
        SpringApplication.run(Application.class, args);
    }
}
