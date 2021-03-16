package com.peter.search.datametrics;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.mq.DeltaDataImportConsumer;
import com.peter.search.pojo.DocData;
import com.peter.search.pojo.LogFormatter;
import com.peter.search.service.SearchService;
import com.peter.search.util.PropertyUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

@Service
public class MetricAgent implements InitializingBean {

    @Autowired
    private SearchService searchService;

    @Autowired
    private PropertyUtils properties;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private DeltaDataImportConsumer deltaDataImport;

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2, new ThreadFactory() {
        private AtomicInteger tag = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("MetricAgent-"+ tag.getAndIncrement());
            return thread;
        }
    });

    @Override
    public void afterPropertiesSet(){
        // 5s发送一次数据，send data to es 记录search-log, search-request
        executorService.scheduleAtFixedRate(new DataMetricsSender(), 1,5, TimeUnit.SECONDS);

        Lock lock = redissonClient.getLock("DataCountMetricsSender");
        if(lock.tryLock()){
            // 监控数据量，1分钟记录一次
            executorService.scheduleAtFixedRate(new DataCountMetricsSender(), 1,60, TimeUnit.SECONDS);
        }
    }

    public void clearDataMetrics(){
        executorService.execute(new DataMetricsSender());
    }

    public class DataMetricsSender implements Runnable {

        public static final String search_request = "search-request";
        public static final String search_log = "search-log";

        @Override
        public void run() {
            List<DocData> docDataList = new ArrayList<>();
            DataMetricFactory.getInstance().getSearchRequestDataMetricMap().forEach((k, v) -> {
                SearchRequestDataMetrics doc;
                while ( ( doc = v.poll()) != null){
                    DocData d = new DocData();
                    d.setDocData(JSON.toJSONString(doc));
                    docDataList.add(d);
                }
            });
            deltaDataImport.indexInsert(search_request, docDataList, new DeltaDataImportConsumer.DeltaESRequestHandler(){
                @Override
                public void failed(Integer failedIndex, String errMsg){
                    dataMetricsService.addFailedCount(serviceTag, 1);
                }
            } );

            List<DocData> docDataList2 = new ArrayList<>();
            DataMetricFactory.getInstance().getDeltaDataMetricMap().forEach((k, v) -> {
                LogFormatter doc;
                while ( ( doc = v.getSyncLog().poll()) != null){
                    doc.setServiceTag(k);
                    DocData d = new DocData();
                    d.setDocData(JSON.toJSONString(doc));
                    docDataList2.add(d);
                }
            });
            deltaDataImport.indexInsert(search_log, docDataList2, new DeltaDataImportConsumer.DeltaESRequestHandler(){
                @Override
                public void failed(Integer failedIndex, String errMsg){
                    dataMetricsService.addFailedCount(serviceTag, 1);
                }
            } );
        }
    }

    public class DataCountMetricsSender implements Runnable {
        public static final String data_count = "data-count";
        public static final String SERVICE_TAG_KEY = "serviceTag";

        @Override
        public void run() {

            String[] allServiceTag = properties.getAllServiceTag();
            for(String serviceTag : allServiceTag){
                JSONObject queryParam = new JSONObject();
                queryParam.put("iRowSize", 0);
                SearchResultDTO searchResultDTO = searchService.search(serviceTag, queryParam);

                JSONObject docDataJson = new JSONObject();
                docDataJson.put(SERVICE_TAG_KEY, serviceTag);
                docDataJson.put("count", searchResultDTO.getTotalNum());
                docDataJson.put("timeMS", System.currentTimeMillis());
                DocData docData = new DocData();
                docData.setDocData(docDataJson.toJSONString());
                docData.setRouting(serviceTag);
                deltaDataImport.indexInsert(data_count, Arrays.asList(docData), new DeltaDataImportConsumer.DeltaESRequestHandler(){
                    @Override
                    public void failed(Integer failedIndex, String errMsg){
                        dataMetricsService.addFailedCount(serviceTag, 1);
                    }
                } );
            }

        }
    }
}
