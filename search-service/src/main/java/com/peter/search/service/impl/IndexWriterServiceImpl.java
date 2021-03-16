package com.peter.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.OP_TYPE;
import com.peter.search.pojo.DocData;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.util.PropertyUtils;
import lombok.Data;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 索引的写操作：insert, delete, update
 * */
@Service
public class IndexWriterServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(IndexWriterServiceImpl.class);

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 3000条数据，每条数据是100KB的话，最多292M*/
    private static final Integer semaphoreSize = 1000;

    private static final Semaphore semaphore = new Semaphore(semaphoreSize);

    private static final ExecutorService executorService = new ThreadPoolExecutor(4, 20,
            3000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
        private AtomicInteger tag = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("写ES索引-"+ tag.getAndIncrement());
            return thread;
        }
    });

    public static final String ERR_MSG = "数据批量更新异常";

    @Autowired
    PropertyUtils properties;
    @Autowired
    ESClientFactory esClientFactory;

    /**
     * 往ES同步数据
     * */
    public void indexInsert(String serviceTag, String indexName, List<DocData> docDataList,
                            boolean create, ESRequestHandler handler) {

        // set Handler
        handler.setDocDataList(docDataList);
        handler.setOpType(OP_TYPE.INSERT.name());
        handler.setServiceTag(serviceTag);

        String typeName = properties.getTypeName(serviceTag);
        BulkRequest bulkRequest = new BulkRequest();
        for(int i=0; i < docDataList.size(); i++){
            IndexRequest indexRequest = new IndexRequest(
                    indexName,
                    typeName,
                    docDataList.get(i).getDocId())
                    .routing(docDataList.get(i).getRouting())
                    .create(create);
            try{
                indexRequest.source(getXContentBuilder(docDataList.get(i)));
            } catch (Exception e){
                handler.failed(i,"ES数据编辑异常"+e.getMessage());
                logger.error("数据编辑异常", e);
            }
            bulkRequest.add(indexRequest);
        }

        submitTaskWithSemaphore(serviceTag, docDataList.size(), handler, bulkRequest);
    }

    /**
     * 往ES同步数据
     * */
    public void indexUpdate(String serviceTag, String indexName, List<DocData> docDataList, boolean upsert,
                            ESRequestHandler handler) {

        handler.setServiceTag(serviceTag);
        handler.setDocDataList(docDataList);
        handler.setOpType(upsert ? OP_TYPE.UPSERT.name() : OP_TYPE.UPDATE.name());

        String typeName = properties.getTypeName(serviceTag);
        BulkRequest bulkRequest = new BulkRequest();
        for(int i=0; i < docDataList.size(); i++){
            UpdateRequest updateRequest = new UpdateRequest(
                    indexName,
                    typeName,
                    docDataList.get(i).getDocId())
                    .routing(docDataList.get(i).getRouting());
            try{
                updateRequest.doc(getXContentBuilder(docDataList.get(i)));
                updateRequest.docAsUpsert(upsert);
            } catch (IOException e){
                handler.failed(i, "解析数据异常："+e.getMessage());
                logger.error("解析数据异常：{}", e.getMessage());
            }
            bulkRequest.add(updateRequest);
        }

        submitTaskWithSemaphore(serviceTag, docDataList.size(), handler, bulkRequest);
    }

    private void submitTaskWithSemaphore(String serviceTag, int dataSize, ESRequestHandler handler, BulkRequest bulkRequest) {
        // 如果数据大于信号量，则无法获取信号，而会永久的被阻塞，所以设为0
        int newDataSize  = dataSize > semaphoreSize ? 0:dataSize;
        try {
            semaphore.acquire(newDataSize);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.submit( () -> {
            bulkRequest(serviceTag, bulkRequest, handler, u -> semaphore.release(dataSize));
            // 如果这样可以控制速率，则说明瓶颈在线程池的任务执行速度，或者是ES客户端异步请求提交速度上
            // 如果以上不是瓶颈，则将release放入callBack方法中,
            // 应该观察采用回调方法之后，是否可以释放docDataList占用的内存
//            semaphore.release(newDataSize);
        });
    }

    /**
     * 解析mapping，并set对应的数据
     * */
    private XContentBuilder getXContentBuilder(DocData docData) throws IOException {

        // 去除转移符 \"
//        String dd = StringEscapeUtils.unescapeJava(docData.getDocData());
//        StringBuilder sb = new StringBuilder();
//        sb.append(dd,0, dd.length()-1);
//        sb.append(",\"esUpdateTime\":\"");
//        sb.append(dateTimeFormatter.format(LocalDateTime.now()));
//        sb.append("\"}");
//        return XContentFactory.jsonBuilder().value(sb.toString());
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            JSONObject docDataJson = JSON.parseObject(docData.getDocData());
            Iterator<Map.Entry<String, Object>> iterator =  docDataJson.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, Object> temp = iterator.next();
                if(docDataJson.get(temp.getKey()) != null) {
                    builder.field(temp.getKey(), docDataJson.get(temp.getKey()));
                }
            }
        }
        // 默认添加esUpdateTime字段，用于数据更新查看使用
        builder.field("esUpdateTime", dateTimeFormatter.format(LocalDateTime.now()));
        builder.endObject();
        return builder;
    }

    /**
     * 批量删除index数据
     * */
    public void indexDelete(String serviceTag, List<DocData> docDataList, ESRequestHandler handler){

        String index = properties.getIndexAlias(serviceTag);
        String type = properties.getTypeName(serviceTag);
        BulkRequest bulkRequest = new BulkRequest();

        docDataList.forEach(docData -> {
            DeleteRequest request = new DeleteRequest(index, type, docData.getDocId());
            request.routing(docData.getRouting());
            bulkRequest.add(request);
        });

        handler.setServiceTag(serviceTag);
        handler.setDocDataList(docDataList);
        handler.setOpType(OP_TYPE.DELETE.name());
        executorService.submit( () -> bulkRequest(serviceTag, bulkRequest, handler, null));
    }


    /**
     * 批量执行ES请求
     * */
    private void bulkRequest(String serviceTag, BulkRequest bulkRequest, ESRequestHandler requestHandler, Consumer<Integer> callback) {

        try{
            RestHighLevelClient restHighLevelClient = esClientFactory.getHighLevelClient(serviceTag);
            restHighLevelClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
                @Override
                public void onResponse(BulkResponse bulkItemResponses) {
                    if(callback != null)callback.accept(bulkRequest.numberOfActions());
                    if(bulkItemResponses.hasFailures()){
                        int index=0;
                        Iterator<BulkItemResponse> it = bulkItemResponses.iterator();
                        while(it.hasNext()){
                            BulkItemResponse bulkItemResponse = it.next();
                            if(bulkItemResponse.isFailed()){
                                requestHandler.failed(index, ERR_MSG +bulkItemResponse.getFailureMessage());
                            } else {
                                requestHandler.success(1);
                            }
                            index++;
                        }
                    } else {
                        requestHandler.success(bulkItemResponses.getItems().length);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if(callback != null)callback.accept(bulkRequest.numberOfActions());
                    requestHandler.failedAll(ERR_MSG+e.getMessage());
                }
            });
        }catch (Exception e){
            if(callback != null)callback.accept(bulkRequest.numberOfActions());
            logger.error(ERR_MSG, e);
            requestHandler.failedAll(ERR_MSG+e.getMessage());
        }
    }

    /**
     * es请求失败后的处理逻辑
     * */
    @Data
    public static abstract class ESRequestHandler {

        protected String serviceTag;
        protected List<DocData> docDataList;
        protected String opType;

        public void failedAll(String errMsg){
            for(int i=0; i < docDataList.size(); i++){
                failed(i, errMsg);
            }
        }

        public abstract void failed(Integer failedIndex, String errMsg);

        public abstract void success(Integer successCount);
    }
}
