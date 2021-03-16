package com.peter.search.controller;

import com.alibaba.fastjson.JSON;
import com.peter.search.annotation.SyncDataMQClient;
import com.peter.search.api.UpdateServiceApi;
import com.peter.search.dao.DataDeleteTriggerDao;
import com.peter.search.dto.OP_TYPE;
import com.peter.search.dto.Result;
import com.peter.search.entity.DeleteTrigger;
import com.peter.search.pojo.DocData;
import com.peter.search.pojo.LogFormatter;
import com.peter.search.util.DeleteJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class IndexDataDeleteTrigger implements InitializingBean {

    private static final String LOCK_NAME = "index-data-delete-lock";
    private static final String THREAD_NAME = "index-data-delete-thread";
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private DataDeleteTriggerDao deleteTriggerDao;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private SyncDataMQClient syncDataMQClient;
    @Autowired
    private UpdateServiceApi updateService;

    @Override
    public void afterPropertiesSet(){
        Thread t = new Thread(() ->{
            while (true){
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                RLock lock = redissonClient.getLock(LOCK_NAME);
                try {
                    //  get lock，如果不加锁，就会重复删除
                    if(lock.tryLock()){
                        startDeleteIndexData();
                    }
                }catch (Exception e){
                    log.error("数据删除异常失败：", e);
                } finally {
                    // release lock
                    lock.unlock();
                }
            }
        });
        t.setName(THREAD_NAME);
        t.setDaemon(true);
        t.start();
    }

    private void startDeleteIndexData() {

        List<DeleteTrigger> triggerList = deleteTriggerDao.getTriggerList();
        for(DeleteTrigger trigger : triggerList){
            try{
                Date now = new Date();
                Date nextTimePoint = new Date();
                if(trigger.getTriggerType() == 1){
                    // 定时触发
                    CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(trigger.getTriggerTime());
                    nextTimePoint = cronSequenceGenerator.next(trigger.getLastTriggerTime());
                } else if(trigger.getTriggerType() == 2){
                    // 周期触发
                    nextTimePoint = DateUtils.addMinutes(trigger.getLastTriggerTime(), Integer.valueOf(trigger.getTriggerTime()));
                }
                if(now.compareTo(nextTimePoint) >= 0){
                    startDelete(trigger.getServiceTag(), trigger.getDeleteJson());
                    deleteTriggerDao.updateLastTriggerTime(trigger.getServiceTag(), now);
                }
            }catch (Exception e){
                log.error("startDeleteIndexData异常：", e);
                logErrMsg(trigger.getServiceTag(), "startDeleteIndexData异常：" + e.getMessage());
            }
        }
    }

    private void startDelete(String serviceTag, String deleteJson) {

        deleteJson = DeleteJsonUtil.handleToken(deleteJson);
        Result rs = updateService.deleteByQuery(serviceTag, deleteJson);
        if(rs.getStatus() == 1) {
            logErrMsg(serviceTag, String.format("deleteByQuery异常：deleteJson:%s, errMsg:%s", deleteJson, rs.getMessage()));
        }
    }

    private void logErrMsg(String serviceTag, String msg){
        try{

            DocData docData = new DocData();
            LogFormatter logData = LogFormatter.builder().hostName(InetAddress.getLocalHost().getHostName())
                    .time(dateTimeFormatter.format(LocalDateTime.now()))
                    .message(msg).build();
            docData.setDocData(JSON.toJSONString(logData));
            syncDataMQClient.syncData(serviceTag, OP_TYPE.INSERT, Arrays.asList(docData));
        }catch (UnknownHostException e ){

        }
    }
}
