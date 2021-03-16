package com.peter.search.dto;

import com.peter.search.pojo.LogFormatter;
import lombok.Data;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class DeltaImportDataMetrics {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SYNC_STATUS syncStatus;
    /** 当前处理的业务*/
    private String serviceTag;
    /** 处理开始时间*/
    private String startTime;
    private AtomicInteger acceptCount;
    private AtomicInteger successCount;
    private AtomicInteger failedCount;
    private ConcurrentLinkedQueue<LogFormatter> syncLog;
    private Integer syncLogMaxSize = 50;
    private Integer failedDataMaxSize = 50;

    public DeltaImportDataMetrics(String serviceTag){
        this.serviceTag = serviceTag;
        this.syncStatus = SYNC_STATUS.SYNC_TASK_STATUS_EMPTY;
        this.syncLog = new ConcurrentLinkedQueue<>();
        syncStart();
    }

    public void addAcceptCount(Integer acceptCount){
        this.acceptCount.addAndGet(acceptCount);
    }

    public void addSuccessCount(Integer successCount){
        this.successCount.addAndGet(successCount);
    }

    public void addFailedCount(Integer failedCount){
        this.failedCount.addAndGet(failedCount);
    }

    public void minusFailedCount(Integer failedCount){
        for(int i = 0; i < failedCount; i++){
            this.failedCount.decrementAndGet();
        }
    }

    /**
     * 数据同步开始
     * */
    public void syncStart(){
        this.syncStatus = SYNC_STATUS.SYNC_TASK_STATUS_BUSY;
        this.startTime = dateTimeFormatter.format(LocalDateTime.now());
        this.acceptCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.failedCount = new AtomicInteger(0);
    }


    public void addLog(String logInfo){
        try {
            this.syncLog.add(LogFormatter.builder().hostName(InetAddress.getLocalHost().getHostName())
                    .time(dateTimeFormatter.format(LocalDateTime.now()))
                    .message(logInfo).build());
        }catch (Exception e){}
        if(syncLog.size() > syncLogMaxSize){
//            syncLog.poll();

        }
    }
    private enum SYNC_STATUS {
        SYNC_TASK_STATUS_BUSY("BUSY", "处理中"),
        SYNC_TASK_STATUS_EMPTY("EMPTY", "空闲");

        private String status;
        private String statusDesc;
        SYNC_STATUS(String status, String statusDesc){
            this.status = status;
            this.statusDesc = statusDesc;
        }
    }

}
