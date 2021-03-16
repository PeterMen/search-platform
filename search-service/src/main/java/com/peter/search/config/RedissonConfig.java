package com.peter.search.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@ConfigurationProperties(prefix = "spring.redis")
@Configuration
public class RedissonConfig{

    /**
     * redis集群部署模式： 1-哨兵模式，2-单点模式，3-集群模式
     * */
    @Value("${spring.redis.cluster.mode}")
    private int clusterMode = 1;
    private int connectionMinimumIdleSize = 4;
    private int idleConnectionTimeout=10000;
    private int pingTimeout=1000;
    private int connectTimeout=10000;
    private int timeout=3000;
    private int retryAttempts=3;
    private int retryInterval=1500;
    private int reconnectionTimeout=3000;
    private int failedAttempts=3;
    private String password = null;
    private int subscriptionsPerConnection=5;
    private String clientName=null;
    private int subscriptionConnectionMinimumIdleSize = 1;
    private int subscriptionConnectionPoolSize = 8;
    private int connectionPoolSize = 10;
    private boolean dnsMonitoring = false;
    private int dnsMonitoringInterval = 5000;

    private int thread = 2; //当前处理核数量 * 2

    private static final String REDIS = "redis://";

    @Bean(destroyMethod = "shutdown")
    RedissonClient redisson(Environment env)  {
        String sentinelMaster = env.getProperty("spring.redis.sentinel.master");
        String sentinelNodes = env.getProperty("spring.redis.sentinel.nodes");
        int database  = Integer.parseInt(env.getProperty("spring.redis.database", "0"));
        String host = env.getProperty("spring.redis.host");
        String port = env.getProperty("spring.redis.port");
        String address = env.getProperty("spring.redis.address");

        Config config = new Config();
        if (clusterMode == 1) {
            // 哨兵模式
            config.useSentinelServers().setMasterName(sentinelMaster)
                    .addSentinelAddress(getRedisNode(sentinelNodes.split(",")))
                    .setMasterConnectionMinimumIdleSize(connectionMinimumIdleSize)
                    .setSlaveConnectionMinimumIdleSize(connectionMinimumIdleSize)
                    .setMasterConnectionPoolSize(5)
                    .setSlaveConnectionPoolSize(5)
                    .setDatabase(database)
                    .setReadMode(ReadMode.SLAVE)
                    .setDnsMonitoringInterval(dnsMonitoringInterval)
                    .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
                    .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                    .setSubscriptionsPerConnection(subscriptionsPerConnection)
                    .setClientName(clientName)
                    .setFailedAttempts(failedAttempts)
                    .setRetryAttempts(retryAttempts)
                    .setRetryInterval(retryInterval)
                    .setReconnectionTimeout(reconnectionTimeout)
                    .setTimeout(timeout)
                    .setConnectTimeout(connectTimeout)
                    .setIdleConnectionTimeout(idleConnectionTimeout)
                    .setPingTimeout(pingTimeout)
                    .setPassword(password);
        } else if (clusterMode == 2) {
            // 单点模式
            config.useSingleServer().setAddress(REDIS + host + ":" + port)
                    .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                    .setConnectionPoolSize(connectionPoolSize)
                    .setDatabase(database)
                    .setDnsMonitoring(dnsMonitoring)
                    .setDnsMonitoringInterval(dnsMonitoringInterval)
                    .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
                    .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                    .setSubscriptionsPerConnection(subscriptionsPerConnection)
                    .setClientName(clientName)
                    .setFailedAttempts(failedAttempts)
                    .setRetryAttempts(retryAttempts)
                    .setRetryInterval(retryInterval)
                    .setReconnectionTimeout(reconnectionTimeout)
                    .setTimeout(timeout)
                    .setConnectTimeout(connectTimeout)
                    .setIdleConnectionTimeout(idleConnectionTimeout)
                    .setPingTimeout(pingTimeout)
                    .setPassword(password);
        } else if (clusterMode == 3) {
            // 集群模式
            config.useClusterServers().addNodeAddress(getRedisNode(address.split(",")))
                    .setMasterConnectionMinimumIdleSize(connectionMinimumIdleSize)
                    .setSlaveConnectionMinimumIdleSize(connectionMinimumIdleSize)
                    .setMasterConnectionPoolSize(connectionPoolSize)
                    .setSlaveConnectionPoolSize(connectionPoolSize)
                    .setDnsMonitoringInterval(dnsMonitoringInterval)
                    .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
                    .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                    .setSubscriptionsPerConnection(subscriptionsPerConnection)
                    .setClientName(clientName)
                    .setFailedAttempts(failedAttempts)
                    .setRetryAttempts(retryAttempts)
                    .setRetryInterval(retryInterval)
                    .setReconnectionTimeout(reconnectionTimeout)
                    .setTimeout(timeout)
                    .setConnectTimeout(connectTimeout)
                    .setIdleConnectionTimeout(idleConnectionTimeout)
                    .setPingTimeout(pingTimeout)
                    .setPassword(password);
        }
        config.setThreads(thread);
        config.setEventLoopGroup(new NioEventLoopGroup());
        config.setUseLinuxNativeEpoll(false);
        return Redisson.create(config);
    }

    private String[] getRedisNode(String[] dd){
        for(int i=0; i < dd.length; i++){
            dd[i] = REDIS + dd[i];
        }
        return dd;
    }
}

