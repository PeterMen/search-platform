package com.peter.search.annotation;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.common.message.Message;
import com.peter.search.dto.OP_TYPE;
import com.peter.search.pojo.DocData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class SyncDataAspect {

    public static final String TOPIC_NAME = "index_data_topic";
    public static final String GROUP_NAME = "index_data_producer_group";

    @Pointcut("@annotation(com.peter.search.annotation.SyncDataPoint)")
    public void pointCut() { }
    
  
    @Autowired
    private Environment env;

    private DefaultMQProducer producer;

    private DefaultMQProducer getMQProducer(){

        if(producer == null){
            synchronized (SyncDataAspect.class){

                DefaultMQProducer producer = new DefaultMQProducer(GROUP_NAME);
                producer.setNamesrvAddr(env.getProperty("rocketmq.producer.namesrvAddr"));
                //如果需要同一个jvm中不同的producer往不同的mq集群发送消息，需要设置不同的instanceName
                //producer.setInstanceName(instanceName);
                if(env.getProperty("rocketmq.producer.maxMessageSize", Integer.class) != null){
                    producer.setMaxMessageSize(env.getProperty("rocketmq.producer.maxMessageSize", Integer.class));
                }
                if(env.getProperty("rocketmq.producer.sendMsgTimeout", Integer.class) !=null){
                    producer.setSendMsgTimeout(env.getProperty("rocketmq.producer.sendMsgTimeout", Integer.class));
                }
                //如果发送消息失败，设置重试次数，默认为2次
                if(env.getProperty("rocketmq.producer.retryTimesWhenSendFailed", Integer.class) !=null ){
                    producer.setRetryTimesWhenSendFailed(env.getProperty("rocketmq.producer.retryTimesWhenSendFailed", Integer.class) );
                }
                try {
                    producer.start();
                    log.info(String.format("producer is start !!! groupName:[%s],namesrvAddr:[%s]"
                            , GROUP_NAME, env.getProperty("rocketmq.consumer.namesrvAddr")));
                } catch (MQClientException e) {
                    log.error(String.format("producer is error {}", e.getMessage(),e));
                }
                this.producer = producer;
            }
        }
        return producer;
    }

    @Around(value="pointCut()")
    public void doAfterReturning(ProceedingJoinPoint point) throws Throwable {

        Signature sig = point.getSignature();
        MethodSignature msig = null;
        if (!(sig instanceof MethodSignature)) {
            throw new IllegalArgumentException("该注解只能用于方法");
        }
        msig = (MethodSignature) sig;
        Object target = point.getTarget();
        Method currentMethod = target.getClass().getMethod(msig.getName(), msig.getParameterTypes());
        // 获取注解参数值
        SyncDataPoint syncDataPoint = currentMethod.getAnnotation(SyncDataPoint.class);
        // 将属性的首字符大写，生成docId的get方法
        String methodName = "get"+syncDataPoint.docIdParamName().substring(0,1).toUpperCase() + syncDataPoint.docIdParamName().substring(1);

        // 执行方法，并获取返回对象
        Object rs = point.proceed();

        // 将返回结果转成docDataList
        List docDataList = new ArrayList<>();
        if(rs instanceof List){
            for(Object rsi : ((List) rs)){
                DocData docData = getDocData(syncDataPoint, methodName, rsi);
                docDataList.add(docData);
            }
        } else {
            docDataList.add(getDocData(syncDataPoint, methodName, rs));
        }

        Message msg = new Message();
        msg.setTopic(TOPIC_NAME);
        msg.setTags(syncDataPoint.serviceTag());
        msg.setKeys(syncDataPoint.opType().toString());
        msg.setBody(JSON.toJSONString(docDataList).getBytes());
        try{
            getMQProducer().send(msg, new DefaultMessageQueueSelector(), syncDataPoint.serviceTag());
        }catch (Exception e){
            log.error("rocketMQ队列数据send失败", e);
        }
    }

    private DocData getDocData(SyncDataPoint syncDataPoint, String methodName, Object rsi) throws IllegalAccessException, InvocationTargetException {
        DocData docData = new DocData();
        docData.setRouting(syncDataPoint.routing());
        docData.setDocDataType(syncDataPoint.docDataType());

        // 设置docId和docData
        if(rsi instanceof String || rsi instanceof Long || rsi instanceof Integer){
            if(syncDataPoint.opType() != OP_TYPE.DELETE){
                throw new RuntimeException("数据删除操作返回类型不支持"+rsi.getClass());
            }
            docData.setDocId(String.valueOf(rsi));
        } else if(rsi instanceof Map){
            // 从map中获取docId
            docData.setDocId(String.valueOf(((Map)rsi).get(syncDataPoint.docIdParamName())));
            docData.setDocData(JSON.toJSONString(rsi));
        } else {
            // 使用get方法获取docId
            Method[] methods = rsi.getClass().getDeclaredMethods();
            for(Method method : methods){
                if(StringUtils.equals(methodName, method.getName())){
                    docData.setDocId(String.valueOf(method.invoke(rsi)));
                    break;
                }
            }
            docData.setDocData(JSON.toJSONString(rsi));
        }
        return docData;
    }
}
