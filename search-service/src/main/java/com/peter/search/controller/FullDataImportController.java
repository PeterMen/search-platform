package com.peter.search.controller;

import com.peter.search.api.FullImportApi;
import com.peter.search.dao.ServiceTagIndexDao;
import com.peter.search.datametrics.DataMetricFactory;
import com.peter.search.datametrics.DataMetricsService;
import com.peter.search.dto.FullImportDTO;
import com.peter.search.dto.Result;
import com.peter.search.service.impl.IndexCreateServiceImpl;
import com.peter.search.service.impl.IndexInfoServiceImpl;
import com.peter.search.service.impl.IndexWriterServiceImpl;
import com.peter.search.thread.DeltaSyncThread;
import com.peter.search.thread.FullDataSyncThread;
import com.peter.search.util.PropertyUtils;
import com.peter.search.util.Constant;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 索引数据全量更新
 *
 * @author 王海涛
 * */
@Api(tags = "数据全量更新接口")
@RestController(value = "fullDataImport")
@RequestMapping("/indexUpdate")
public class FullDataImportController implements FullImportApi {

    private static final Logger logger = LoggerFactory.getLogger(FullDataImportController.class);
    public static final String FAILED_MSG = "郁闷，数据同步失败了！！！";
    public static final String SUCCESS_MSG = "恭喜你，数据同步成功了！！！";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IndexCreateServiceImpl indexCreator;
    @Autowired
    private IndexInfoServiceImpl indexInfo;
    @Autowired
    private IndexWriterServiceImpl indexWriter;
    @Autowired
    @Qualifier(value = "fullDataMetricsService")
    private DataMetricsService dataMetricsService;
    @Autowired
    private ServiceTagIndexDao serviceTagIndexDao;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private PropertyUtils properties;

    /**
     * 索引全量更新
     *
     * */
    @ApiOperation(value = "索引全量更新", notes = "索引全量更新",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "fullImportData", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Result fullImportData(@RequestBody FullImportDTO param) {

        String serviceTag = param.getServiceTag();

        // 同步任务开始。。。。
        if(CollectionUtils.isEmpty(param.getExtraParams())){
            // 新建索引，全量数据同步
            startSyncThread(serviceTag, param, this::startFullImport);
        } else {
            // 更新部分数据
            startSyncThread(serviceTag, param, this::startSyncDataToCurrentIndex);
        }

        return Result.buildSuccess("索引同步开始....");
    }

    private void startSyncThread(String serviceTag, FullImportDTO param, Function<FullImportDTO, String> sync){
        Thread t = new Thread(()-> {
            RLock lock = redissonClient.getLock(Constant.FULL_IMPORT_LOCK+serviceTag);
            // 加锁，数据同步任务开始执行。。。
            if(!lock.tryLock()){
                dataMetricsService.metricsLog(serviceTag, "全量数据导入锁lock failed");
            }
            try{
                DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).syncStart();
                String message = sync.apply(param);
                dataMetricsService.metricsLog(serviceTag, message);
            } finally {
                // 释放全局锁
                lock.forceUnlock();
                DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).syncFinish();
            }
        });
        t.setName("full-import-thread");
        t.start();
    }

    /**
     * 索引同步主流程
     * */
    private String startFullImport(FullImportDTO param) {
        String serviceTag = param.getServiceTag();
        String fullImportUrl = param.getFullImportUrl();
        String newIndexName = param.getNewIndexName();

        dataMetricsService.metricsLog(serviceTag, serviceTag+"索引全量同步开始。。。");
        dataMetricsService.metricsLog(serviceTag, "新的索引名称为：" + newIndexName);
        try{
            // step1:停止copy Msg消费，并开始copy msg
            stringRedisTemplate.convertAndSend(Constant.COPY_MSG_SERVICE_TAG_STOP, serviceTag);
            stringRedisTemplate.convertAndSend(Constant.COPY_MSG_SERVICE_TAG_ADD, serviceTag);

            // step3:为了加快reindex的速度，先设置index.refresh_interval为-1
            indexCreator.updateRefreshInterval(serviceTag, newIndexName, "-1");

            // step4:读取数据源数据，并同步到ES
            syncDataToNewIndex(serviceTag, newIndexName, fullImportUrl, param.getPageSize(), param.getUseFeign());

            // step5:恢复index.refresh_interval为1
            indexCreator.updateRefreshInterval(serviceTag, newIndexName, "1s");

            // step6:检查新索引的可用性
            dataMetricsService.metricsLog(serviceTag, "索引可用性检查...");
            if(!indexCreator.checkIndexAvailable(serviceTag, newIndexName)){
                dataMetricsService.metricsLog(serviceTag, "索引可用性检查未通过，索引不切换，indexName:"+newIndexName);
                DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).syncFinish();
                // step7:停止copy msg
                stringRedisTemplate.convertAndSend(Constant.COPY_MSG_SERVICE_TAG_REMOVE, serviceTag);
                return FAILED_MSG;
            }
            dataMetricsService.metricsLog(serviceTag, "索引可用性检查通过，indexName:"+newIndexName);

            // step8:切换索引
            dataMetricsService.metricsLog(serviceTag, "给索引切换别名。。。");
            indexCreator.createAliasForIndex(serviceTag, newIndexName, properties.getIndexAlias(serviceTag));

            // step9:停止copy msg
            stringRedisTemplate.convertAndSend(Constant.COPY_MSG_SERVICE_TAG_REMOVE, serviceTag);

            // step10:开始消费copy msg
            stringRedisTemplate.convertAndSend(Constant.COPY_MSG_SERVICE_TAG_START, serviceTag);
            stringRedisTemplate.convertAndSend(Constant.RELOAD_MAPPING_PROPERTIES, serviceTag);

            // step11: delete the oldest index
            serviceTagIndexDao.deleteOldOne(serviceTag);

            logger.info("全量索引同步完成，indexName:{}", newIndexName);
            dataMetricsService.metricsLog(serviceTag, newIndexName+"全量索引同步完成");
        } catch (Exception e) {
            logger.error("全量导入异常：serviceTag：{}", serviceTag , e);
            dataMetricsService.metricsLog(serviceTag, "全量导入异常：" + e.getMessage());
            return FAILED_MSG;
        }
        return SUCCESS_MSG;
    }

    private void syncDataToNewIndex(String serviceTag, String newIndexName, String fullImportUrl, Integer pageSize, Boolean useFeign){

        int nThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
            private  AtomicInteger tag = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("拉取数据线程-"+ tag.getAndIncrement());
                return thread;
            }
        });
        List<Future<Long>> timeSpendList = new ArrayList<>(10);
        FullDataSyncThread task = new FullDataSyncThread();
        task.setDataImportUrl(fullImportUrl);
        task.setPageSize(pageSize);
        task.setServiceTag(serviceTag);
        task.setDataMetricsService(dataMetricsService);
        task.setIndexName(newIndexName);
        task.setIndexWriter(indexWriter);
        task.setRestTemplate(useFeign ? restTemplate : new RestTemplate());
        task.initTotalPage();
        for(int i = 0; i < nThreads; i++ ){
            timeSpendList.add( executorService.submit(task));
        }
        Long timeSpend = timeSpendList.stream().mapToLong(taskFuture -> {
            try {
                return taskFuture.get();
            } catch (InterruptedException|ExecutionException e) {
                logger.error("异常", e);
            }
            return 0L;
        }).max().getAsLong();
        executorService.shutdown();
        dataMetricsService.metricsLog(serviceTag, "数据同步结束");
        dataMetricsService.metricsLog(serviceTag, "获取数据耗时(单位ms)："+timeSpend);
        logger.info("获取数据耗时：{}", timeSpend);
    }


    /**
     * 索引同步主流程
     * */
    private String startSyncDataToCurrentIndex(FullImportDTO param){

        String serviceTag = param.getServiceTag();
        String fullImportUrl = param.getFullImportUrl();
        dataMetricsService.metricsLog(serviceTag, serviceTag+"数据同步开始。。。");
        try {
            int nThreads = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
                private  AtomicInteger tag = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("拉取数据线程-"+ tag.getAndIncrement());
                    return thread;
                }
            });
            List<Future<Long>> timeSpendList = new ArrayList<>(10);
            DeltaSyncThread task = new DeltaSyncThread();
            String indexName = indexInfo.getIndexNameByAlias(serviceTag, properties.getIndexAlias(serviceTag));
            task.setIndexName(indexName);
            task.setPageSize(param.getPageSize());
            task.setDataImportUrl(fullImportUrl);
            task.setServiceTag(serviceTag);
            task.setDataMetricsService(dataMetricsService);
            task.setIndexWriter(indexWriter);
            task.setRestTemplate(param.getUseFeign() ? restTemplate : new RestTemplate());
            task.setExtraParams(param.getExtraParams());
            task.initTotalPage();
            for(int i = 0; i < nThreads; i++ ){
                timeSpendList.add( executorService.submit(task));
            }
            Long timeSpend = timeSpendList.stream().mapToLong(taskFuture -> {
                try {
                    return taskFuture.get();
                } catch (InterruptedException|ExecutionException e) {
                    logger.error("异常", e);
                }
                return 0L;
            }).sum();
            executorService.shutdown();
            dataMetricsService.metricsLog(serviceTag, "数据同步结束");
            dataMetricsService.metricsLog(serviceTag, "获取数据耗时(单位ms)："+timeSpend);
            logger.info("获取数据耗时：{}", timeSpend);

//            // 同步结果检查
//            Integer successCount = DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).getSuccessCount().get();
//            Integer failedCount = DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).getFailedCount().get();
//            Integer totalCount = DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).getTotalCount();
//            if(totalCount-successCount > 10 || failedCount > 10){
//                dataMetricsService.metricsLog(serviceTag, "文档错误数过多。");
//                return FAILED_MSG;
//            }

        } catch (Exception e) {
            logger.error("数据同步异常：", e);
            dataMetricsService.metricsLog(serviceTag, "数据同步异常："+e.getMessage());
            return FAILED_MSG;
        }
        return SUCCESS_MSG;
    }
}
