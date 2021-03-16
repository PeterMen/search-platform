package com.peter.search.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.api.SearchServiceApi;
import com.peter.search.pojo.LogFormatter;
import com.peter.search.dto.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import tk.mybatis.mapper.util.Assert;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Api(tags = "同步状态查询接口", position=7)
@RestController(value = "dataMetrics")
@RequestMapping("/dataMetrics")
public class DataMetricsController {

    @Autowired
    DiscoveryClient discoveryClient;

    @Autowired
    SearchServiceApi searchService;

    @Value("${search.application.name:search-service}")
    private String appName;

    /**
     * 查询全量更新状态
     * */
    @ApiOperation(value = "查询全量更新状态", notes = "查询全量更新状态",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceTag", value = "业务标识：APP、H5、PC", required = true, dataType = "string", paramType = "query")
    })
    @GetMapping(value = "getFullImportDataMetrics")
    public Result getFullImportDataMetrics(@RequestParam String serviceTag) throws ParseException {
        Assert.notEmpty(serviceTag, "serviceTag不能为空");
        Object rs = new FullImportDataMetrics(serviceTag);
        List<ServiceInstance> instances = discoveryClient.getInstances(appName);
        String finishTime = null;
        for(ServiceInstance instance : instances){
            ResponseEntity<String> responseEntity = new RestTemplate()
                    .getForEntity(instance.getUri()+"/"+appName+"/dataMetrics/getFullImportDataMetricsSelf?serviceTag="+serviceTag, String.class);
            JSONObject rsJson = JSON.parseObject(responseEntity.getBody());
            if(StringUtils.equals(rsJson.getString("syncStatus"), FullImportDataMetrics.SYNC_STATUS.SYNC_TASK_STATUS_BUSY.name())){
                rs = rsJson;
                break;
            }
            if(StringUtils.isEmpty(rsJson.getString("finishTime"))){
                continue;
            }
            if(StringUtils.isEmpty(finishTime)
                    || DateUtils.parseDate(rsJson.getString("finishTime"), "yyyy-MM-dd HH:mm:ss") .compareTo(
                    DateUtils.parseDate(finishTime, "yyyy-MM-dd HH:mm:ss")  )  > 0){
                rs = rsJson;
                finishTime = rsJson.getString("finishTime");
            }
        }
        return Result.buildSuccess(rs);
    }

    /**
     * 查询全量更新状态
     * */
    @ApiOperation(value = "查询增量更新状态", notes = "查询全量更新状态",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceTag", value = "业务标识：APP、H5、PC", required = true, dataType = "string", paramType = "query")
    })
    @GetMapping(value = "getDeltaImportDataMetrics")
    public Result getDeltaImportDataMetrics(@RequestParam String serviceTag, @RequestParam(required = false) Integer logSize){
        Assert.notEmpty(serviceTag, "serviceTag不能为空");
        if(logSize==null) logSize = 50;
        DeltaImportDataMetrics rs = new DeltaImportDataMetrics(serviceTag);
        List<ServiceInstance> instances = discoveryClient.getInstances(appName);
        for(ServiceInstance instance : instances){
            ResponseEntity<String> responseEntity = new RestTemplate()
                    .getForEntity(instance.getUri()+"/"+appName+"/dataMetrics/getDeltaImportDataMetricsSelf?serviceTag="+serviceTag, String.class);
            JSONObject rsJson  =JSON.parseObject(responseEntity.getBody());
            rs.addAcceptCount(rsJson.getInteger("acceptCount"));
            rs.addFailedCount(rsJson.getInteger("failedCount"));
            rs.addSuccessCount(rsJson.getInteger("successCount"));
            rs.setStartTime(rsJson.getString("startTime"));
        }

        JSONObject query = new JSONObject();
        query.put("iRowSize", logSize);
        query.put("serviceTag", serviceTag);
        query.put("sSort", "time desc");
        SearchResultDTO resultDTO = searchService.search(new SearchRequestDTO<>("search-log", query));
        ConcurrentLinkedQueue<LogFormatter> linkedQueue = new ConcurrentLinkedQueue();
        for(Map m : resultDTO.getData()){
            linkedQueue.add(LogFormatter.builder()
                            .hostName(String.valueOf(m.get("hostName")))
                            .message(String.valueOf(m.get("message")))
                            .time(String.valueOf(m.get("time"))).build());
        }
        rs.setSyncLog(linkedQueue);
        return Result.buildSuccess(rs);
    }

    @GetMapping(value = "clearDeltaImportDataMetrics")
    public Result clearDeltaImportDataMetrics(@RequestParam String serviceTag) {
        Assert.notEmpty(serviceTag, "serviceTag不能为空");
        List<ServiceInstance> instances = discoveryClient.getInstances(appName);
        for (ServiceInstance instance : instances) {
            new RestTemplate()
                    .getForEntity(instance.getUri() + "/" + appName + "/dataMetrics/clearDeltaImportDataMetricsSelf?serviceTag=" + serviceTag, Result.class);
        }
        return Result.buildSuccess();
    }
}
