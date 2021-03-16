package com.peter.search.mq;

import com.peter.search.datametrics.DataMetricsService;
import com.peter.search.pojo.DocData;
import com.peter.search.service.impl.IndexWriterServiceImpl;
import com.peter.search.util.PropertyUtils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 包装了dataMetricsService，具有metrics的能力
 * 缓存了properties
 * */
@Component
public class DeltaDataImportConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DeltaDataImportConsumer.class);
    @Autowired
    private IndexWriterServiceImpl indexWriter;
    @Autowired
    @Qualifier(value = "deltaDataMetricsService")
    private DataMetricsService dataMetricsService;
    @Autowired
    private PropertyUtils properties;

    /**
     * 索引增量更新消费队列
     * */
    public void indexUpdate(String serviceTag, List<DocData> docDataList, boolean upsert, DeltaESRequestHandler handler){

        try{
            String indexAlias = properties.getIndexAlias(serviceTag);
            handler.setDataMetricsService(dataMetricsService);
            indexWriter.indexUpdate(serviceTag, indexAlias, docDataList, upsert, handler);
        }catch (Exception e){
            logger.error("增量数据同步失败：serviceTag={}", serviceTag, e);
            dataMetricsService.metricsLog(serviceTag, e.getMessage());
            dataMetricsService.addFailedCount(serviceTag, docDataList.size());
            throw e;
        }
    }


    /**
     * 索引增量更新消费队列
     * */
    public void indexInsert(String serviceTag, List<DocData> docDataList, DeltaESRequestHandler handler){

        try{
            String indexAlias = properties.getIndexAlias(serviceTag);
            handler.setDataMetricsService(dataMetricsService);
            indexWriter.indexInsert(serviceTag, indexAlias, docDataList, false, handler);
        }catch (Exception e){
            logger.error("增量数据同步失败：", e);
            dataMetricsService.metricsLog(serviceTag, e.getMessage());
            dataMetricsService.addFailedCount(serviceTag, docDataList.size());
            throw e;
        }
    }

    /**
     * 索引增量更新消费队列
     * */
    public void indexDelete(String serviceTag, List<DocData> docDataList, DeltaESRequestHandler handler){

        try{
            handler.setDataMetricsService(dataMetricsService);
            indexWriter.indexDelete(serviceTag, docDataList, handler);
        }catch (Exception e){
            logger.error("增量数据同步失败：", e);
            dataMetricsService.metricsLog(serviceTag, e.getMessage());
            dataMetricsService.addFailedCount(serviceTag, docDataList.size());
            throw e;
        }
    }


    @Data
    public abstract static class DeltaESRequestHandler extends IndexWriterServiceImpl.ESRequestHandler{
        protected DataMetricsService dataMetricsService;

        @Override
        public void success(Integer successCount){
            dataMetricsService.addSuccessCount(serviceTag, successCount);
        }
    }
}
