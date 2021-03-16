package com.peter.search.dto;

import com.peter.search.pojo.LogFormatter;
import lombok.Data;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class FullImportDataMetrics {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SYNC_STATUS syncStatus;
    /** 当前处理的业务*/
    private String serviceTag;
    /** 处理开始时间*/
    private String startTime;
    /** 处理结束时间*/
    private String finishTime;
    private String lastFinishTime;
    private Integer totalCount;
    private AtomicInteger acceptCount;
    private AtomicInteger successCount;
    private AtomicInteger failedCount;
    private ConcurrentLinkedQueue<ImmutablePair<String, String>> failedDataAndReason;
    private ConcurrentLinkedQueue<LogFormatter> syncLog;
    private Integer syncLogMaxSize = 50;
    private Integer failedDataMaxSize = 50;

    public FullImportDataMetrics(String serviceTag){
        this.serviceTag = serviceTag;
    }

    public void addSuccessCount(Integer successCount){
        this.successCount.addAndGet(successCount);
    }

    public void addAcceptCount(Integer acceptCount){
        this.acceptCount.addAndGet(acceptCount);
    }

    public void addFailedCount(Integer failedCount){
        this.failedCount.addAndGet(failedCount);
    }

    /**
     * 数据同步开始
     * */
    public void syncStart(){
        this.syncStatus = SYNC_STATUS.SYNC_TASK_STATUS_BUSY;
        this.startTime = dateTimeFormatter.format(LocalDateTime.now());
        this.lastFinishTime = this.finishTime;
        this.finishTime = null;
        this.totalCount = 0;
        this.acceptCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.failedCount = new AtomicInteger(0);
        this.failedDataAndReason = new ConcurrentLinkedQueue<>();
        this.syncLog = new ConcurrentLinkedQueue<>();
    }

    /**
     * 数据同步结束
     * */
    public void syncFinish(){
        this.syncStatus = SYNC_STATUS.SYNC_TASK_STATUS_EMPTY;
        this.finishTime = dateTimeFormatter.format(LocalDateTime.now());
    }

    public void addLog(String logInfo){
        try {
            this.syncLog.add(LogFormatter.builder().hostName(InetAddress.getLocalHost().getHostName())
                    .time(dateTimeFormatter.format(LocalDateTime.now()))
                    .message(logInfo).build());
        }catch (Exception e){}
        if(syncLog.size() > syncLogMaxSize){
            syncLog.poll();
        }
    }

    public void addFailedData(String failedData, String failedReason){
        this.failedDataAndReason.add(ImmutablePair.of(failedReason, failedData));
        if(failedDataAndReason.size() > failedDataMaxSize){
            failedDataAndReason.poll();
        }
    }

    public enum SYNC_STATUS {
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
