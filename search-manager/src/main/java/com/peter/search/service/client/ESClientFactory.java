package com.peter.search.service.client;

import com.peter.search.service.impl.ServiceTagInfoImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 *  Es client Factory
 *
 * @author 王海涛
 * */
@Component
public class ESClientFactory {


    private static final Logger logger = LoggerFactory.getLogger(ESClientFactory.class);

    private static final String ES_HOSTS = ".cluster.address";
    private static final String SCHEMA = "http";
    private static final String CONNECT_TIME_OUT = "es.connect.time.out.seconds";
    private static final String SOCKET_TIME_OUT = "es.socket.time.out.seconds";
    private static final String CONNECTION_REQUEST_TIME_OUT = "es.request.time.out.seconds";
    private static final String MAX_CONNECT_NUM = "es.max.connect.num";
    private static final String MAX_CONNECT_PER_ROUTE = "es.connect.per.route";

    private static final Integer REST_CLIENT = 1;
    private static final Integer HIGH_LEVEL_CLIENT = 2;
    private static boolean uniqueConnectTimeConfig = false;
    private static boolean uniqueConnectNumConfig = true;

    private static Map<String, RestHighLevelClient> esHLClientMapping = new HashMap<>(16);
    private static Map<String, RestClient> esRestClientMapping = new HashMap<>(16);

    @Autowired
    private Environment env;

    @Autowired
    private ServiceTagInfoImpl serviceTagInfo;

    /**
     * 初始化集群连接
     * @param initType 初始化client类型
     * @param esTag es集群
     * */
    public void init(Integer initType, String esTag){

        //如果本地未缓存，则加载
        String serverCluster = env.getProperty(esTag + ES_HOSTS);
        if(StringUtils.isEmpty(serverCluster)){
            throw  new RuntimeException(esTag+"地址未配置");
        }
        String[] servers = serverCluster.split(";");
        HttpHost [] hosts = new HttpHost[servers.length];
        int i = 0;
        for(String server:servers) {
            String[] sp = server.split(":");
            hosts[i] = new HttpHost(sp[0], Integer.parseInt(sp[1]), SCHEMA);
            i++;
        }
        RestClientBuilder builder = RestClient.builder(hosts);

        if(uniqueConnectTimeConfig){
            setConnectTimeOutConfig(builder);
        }
        if(uniqueConnectNumConfig){
            setMutiConnectConfig(builder);
        }
        if(REST_CLIENT.equals(initType)){
            esRestClientMapping.put(esTag, builder.build());
        }
        if(HIGH_LEVEL_CLIENT.equals(initType)){
            esHLClientMapping.put(esTag, new RestHighLevelClient(builder));
        }
    }

    /**
     *     主要关于异步httpclient的连接延时配置
     */

    public void setConnectTimeOutConfig(RestClientBuilder builder){
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setConnectTimeout(Integer.valueOf(env.getProperty(CONNECT_TIME_OUT)));
            requestConfigBuilder.setSocketTimeout(Integer.valueOf(env.getProperty(SOCKET_TIME_OUT)));
            requestConfigBuilder.setConnectionRequestTimeout(Integer.valueOf(env.getProperty(CONNECTION_REQUEST_TIME_OUT)));
            return requestConfigBuilder;
        });
    }
    /**
     *    主要关于异步httpclient的连接数配置
     */
    public void setMutiConnectConfig(RestClientBuilder builder){
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(Integer.valueOf(env.getProperty(MAX_CONNECT_NUM)));
            httpClientBuilder.setMaxConnPerRoute(Integer.valueOf(env.getProperty(MAX_CONNECT_PER_ROUTE)));
            // 长连接保持55s
            httpClientBuilder.setKeepAliveStrategy((response, context) -> 55000);
            return httpClientBuilder;
        });
    }

    /**
     * 懒加载
     * */
    public RestClient getRestClient(String serviceTag){
        return getHighLevelClient(serviceTag).getLowLevelClient();
    }

    /**
     * 懒加载
     * */
    public RestHighLevelClient getHighLevelClient(String serviceTag){
        String esTag = serviceTagInfo.getEsName(serviceTag);
        if(StringUtils.isEmpty(esTag)){
            throw new RuntimeException(serviceTag+"_es集群地址未配置。");
        }
        if(esHLClientMapping.get(esTag) == null){
            synchronized (ESClientFactory.class){
                if(esHLClientMapping.get(esTag) == null){
                    init(HIGH_LEVEL_CLIENT, esTag);
                }
            }
        }
        return esHLClientMapping.get(esTag);
    }

    public void close() {
        esHLClientMapping.forEach((esTag, esHighLevelClient) -> {
            if (esHighLevelClient != null) {
                try {
                    esHighLevelClient.close();
                } catch (Exception e) {
                    logger.error("ES连接失败：", e);
                }
            }
        });
    }
}
