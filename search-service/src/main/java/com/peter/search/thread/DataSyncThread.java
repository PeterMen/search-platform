package com.peter.search.thread;

import com.alibaba.fastjson.JSON;
import com.peter.search.datametrics.DataMetricsService;
import com.peter.search.pojo.FetchData;
import com.peter.search.service.impl.IndexWriterServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class DataSyncThread implements Callable<Long> {

    private Integer pageSize = 50;
    /** pull data from page 1 */
    private AtomicInteger currentPage = new AtomicInteger(0);
    private AtomicInteger totalPage = new AtomicInteger(1000);
    private RestTemplate restTemplate;
    protected DataMetricsService dataMetricsService;
    private String dataImportUrl;
    protected String serviceTag;
    protected String indexName;
    protected IndexWriterServiceImpl indexWriter;
    private Map<String, String> extraParams;

    public Long run() {

        Long time = 0L;

        while (true){
            // step3:分页查询serviceTag对应的服务方提供的全量数据
            Integer myPage = currentPage.incrementAndGet();
            if(myPage > totalPage.get()){
                // 数据拉取结束
                return time;
            }
            long startTime = System.currentTimeMillis();
            FetchData fetchData = fetchData(pageSize, myPage);
            time+=(System.currentTimeMillis()-startTime);

            // 收到数据
            dataMetricsService.addAcceptCount(serviceTag, fetchData.getDataList().size());
            if(totalPage.get() != fetchData.getPage().getTotalPage()){
                // 刷新totalPage
                totalPage.set(fetchData.getPage().getTotalPage());
            }
            dataMetrics(fetchData);
            try{
                // step4:批量更新
                indexWrite(fetchData);
            }catch (Exception e){
                log.error("ES数据导入异常：",  e);
                dataMetricsService.metricsLog(serviceTag, "ES数据导入异常："+e.getMessage());
                dataMetricsService.addFailedCount(serviceTag, fetchData.getDataList().size());
            }
        }
    }

    /**
     * 读到数据之后，如何写索引
     * */
    protected abstract void indexWrite(FetchData fetchData);

    /**
     * 统计
     * */
    protected abstract void dataMetrics(FetchData fetchData);

    public void initTotalPage(){
        FetchData fetchData = fetchData(pageSize, 1);
        this.totalPage.set(fetchData.getPage().getTotalPage());
    }

    /**
     * 从数据源拉取数据
     * * 规定接口返回格式如下
     *          * {page:{totalPage:10,
     *          * currentPage:1,
     *          * totalCount:570},
     *          * dataList:[{
     *          *     docId:2323,
     *          *     docData:{......},
     *          *     docDataType:json
     *          * }]
     *          * }
     * */
    private FetchData fetchData(Integer pageSize, Integer currentPage) {

        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("pageSize", pageSize);
        paramMap.add("currentPage", currentPage);
        if(!CollectionUtils.isEmpty(extraParams)){
            extraParams.forEach(paramMap::add);
        }

        try{
            ResponseEntity<FetchData> responseEntity = restTemplate.postForEntity(dataImportUrl, paramMap, FetchData.class);
            FetchData fetchData = responseEntity.getBody();

            // response handler
            if(fetchData == null || fetchData.getPage() == null || CollectionUtils.isEmpty(fetchData.getDataList())){
                dataMetricsService.metricsLog(serviceTag, "该获取数据返回有异常, httpCode:"+responseEntity.getStatusCodeValue()+",currentPage:"+currentPage+",pageSize:"+pageSize+",fetchData："+ JSON.toJSONString(fetchData));
                dataMetricsService.addFailedCount(serviceTag, pageSize);
                throw new NullPointerException("返回数据fetchData为空");
            }
            return fetchData;
        } catch (Exception e){
            dataMetricsService.metricsLog(serviceTag, "数据拉取异常了, currentPage:"+currentPage+",pageSize:"+pageSize+",errMsg："+ e.getMessage());
            log.error("数据拉取异常了：", e);
            throw new IllegalStateException("数据读取失败！停止全量数据同步");
        }
    }

    @Override
    public Long call() throws Exception {
        return run();
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setDataMetricsService(DataMetricsService dataMetricsService) {
        this.dataMetricsService = dataMetricsService;
    }

    public void setDataImportUrl(String dataImportUrl) {
        this.dataImportUrl = dataImportUrl;
    }

    public void setServiceTag(String serviceTag) {
        this.serviceTag = serviceTag;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public void setIndexWriter(IndexWriterServiceImpl indexWriter) {
        this.indexWriter = indexWriter;
    }

    public void setTotalPage(Integer totalPage) {
        this.totalPage.set(totalPage);
    }

    public void setExtraParams(Map<String, String> extraParams) {
        this.extraParams = extraParams;
    }

    public void setPageSize(Integer pageSize) {
        if(pageSize == null || pageSize <= 0){
            this.pageSize = 50;
        }
        else if(pageSize > 200){
            this.pageSize = 200;
        }
        else {
            this.pageSize = pageSize;
        }
    }
}
