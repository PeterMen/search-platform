package com.peter.search.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.api.SearchServiceApi;
import com.peter.search.dto.Result;
import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.pojo.AggregationParam;
import com.peter.search.pojo.RangeAggregation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "dashboard", position=10)
@RestController(value = "dashboard")
@RequestMapping("/dashboard")
public class DashBoardController {

    @Autowired
    private SearchServiceApi searchService;

    @ApiOperation(value = "统计请求数", notes = "统计请求数",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceTag", value = "serviceTag",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "startTime", value = "时间格式：yyyy-MM-dd HH:mm:ss",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "endTime", value = "时间格式：yyyy-MM-dd HH:mm:ss",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "timeUnit", value = "1-天,2-小时,3-分,4-秒",  dataType = "string", paramType = "query")
    })
    @GetMapping(value = "/requestCount")
    public Result requestCount(@RequestParam String serviceTag, @RequestParam String startTime, @RequestParam String endTime,
                               @RequestParam Integer timeUnit) {

        Integer size = 60;
        Long gap = getGapMills(timeUnit);
        Long startTimeMS = getTimeMS(startTime);
        Long endTimeMS = getTimeMS(endTime);

        JSONObject queryParam = new JSONObject();
        queryParam.put("serviceTag", serviceTag);
        queryParam.put("timeMS", "gte:"+startTimeMS);
        queryParam.put("timeMS", "lte:"+endTimeMS);
        RangeAggregation aggregation = new RangeAggregation();
        aggregation.setRangeType(RangeAggregation.RangeType.general);
        aggregation.setField("timeMS");
        List<RangeAggregation.Range> ranges = new ArrayList<>();
        for(int i=0; i < size; i++){
            RangeAggregation.Range r = new RangeAggregation.Range();
            r.setKey(String.valueOf(i));
            r.setFrom(String.valueOf(startTimeMS + i*gap));
            r.setTo(String.valueOf(startTimeMS + (i+1)*gap));
            ranges.add(r);
        }
        aggregation.setRanges(ranges);
        queryParam.put("sRangeAgg", JSON.toJSON(aggregation));
        SearchResultDTO r = searchService.search(new SearchRequestDTO("search-request", queryParam));

        if(r.getAggregationDataMap().containsKey("timeMS")){
            List<SearchResultDTO.AggregationData> list = r.getAggregationDataMap().get("timeMS");
            Map<String, Long> resultMap = new HashMap<>(50);
            list.forEach(k -> resultMap.put(k.getKey(), k.getCount()));
            return Result.buildSuccess(resultMap);
        }
        return Result.buildErr("失败");
    }

    @ApiOperation(value = "统计平均耗时", notes = "统计平均耗时",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/requestAvgTimeMS")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceTag", value = "serviceTag",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "startTime", value = "时间格式：yyyy-MM-dd HH:mm:ss",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "endTime", value = "时间格式：yyyy-MM-dd HH:mm:ss",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "timeUnit", value = "1-天,2-小时,3-分,4-秒",  dataType = "string", paramType = "query")
    })
    public Result requestAvgTimeMS(@RequestParam String serviceTag, @RequestParam String startTime, @RequestParam String endTime,
                                   @RequestParam Integer timeUnit) {

        Integer size = 60;
        Long gap = getGapMills(timeUnit);
        Long startTimeMS = getTimeMS(startTime);
        Long endTimeMS = getTimeMS(endTime);

        JSONObject queryParam = new JSONObject();
        queryParam.put("serviceTag", serviceTag);
        queryParam.put("timeMS", "gte:"+startTimeMS);
        queryParam.put("timeMS", "lte:"+endTimeMS);
        RangeAggregation aggregation = new RangeAggregation();
        aggregation.setRangeType(RangeAggregation.RangeType.general);
        aggregation.setField("timeMS");
        List<RangeAggregation.Range> ranges = new ArrayList<>();
        for(int i=0; i < size; i++){
            RangeAggregation.Range r = new RangeAggregation.Range();
            r.setKey(String.valueOf(i));
            r.setFrom(String.valueOf(startTimeMS + i*gap));
            r.setTo(String.valueOf(startTimeMS + (i+1)*gap));
            ranges.add(r);
        }
        aggregation.setRanges(ranges);
        aggregation.setFunctionField("spendTimeMS");
        aggregation.setFunction(AggregationParam.FUNCTION_ENUM.avg);
        queryParam.put("sRangeAgg", JSON.toJSON(aggregation));
        SearchResultDTO r = searchService.search(new SearchRequestDTO("search-request", queryParam));

        if(r.getAggregationDataMap().containsKey("timeMS")){
            List<SearchResultDTO.AggregationData> list = r.getAggregationDataMap().get("timeMS");
            Map<String, Double> resultMap = new HashMap<>(100);
            list.forEach(k -> resultMap.put(k.getKey(), Double.isInfinite(k.getAvg()) ? 0 : k.getAvg()));
            return Result.buildSuccess(resultMap);
        }
        return Result.buildErr("失败");
    }

    @ApiOperation(value = "统计请求异常数", notes = "统计请求异常数",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/requestExceptionCount")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceTag", value = "serviceTag",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "startTime", value = "时间格式：yyyy-MM-dd HH:mm:ss",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "endTime", value = "时间格式：yyyy-MM-dd HH:mm:ss",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "timeUnit", value = "1-天,2-小时,3-分,4-秒",  dataType = "string", paramType = "query")
    })
    public Result requestExceptionCount(@RequestParam String serviceTag, @RequestParam String startTime, @RequestParam String endTime,
                                   @RequestParam Integer timeUnit) {

        Integer size = 60;
        Long gap = getGapMills(timeUnit);
        Long startTimeMS = getTimeMS(startTime);
        Long endTimeMS = getTimeMS(endTime);

        JSONObject queryParam = new JSONObject();
        queryParam.put("serviceTag", serviceTag);
        queryParam.put("hasException", true);
        queryParam.put("timeMS", "gte:"+startTimeMS);
        queryParam.put("timeMS", "lte:"+endTimeMS);
        RangeAggregation aggregation = new RangeAggregation();
        aggregation.setRangeType(RangeAggregation.RangeType.general);
        aggregation.setField("timeMS");
        List<RangeAggregation.Range> ranges = new ArrayList<>();
        for(int i=0; i < size; i++){
            RangeAggregation.Range r = new RangeAggregation.Range();
            r.setKey(String.valueOf(i));
            r.setFrom(String.valueOf(startTimeMS + i*gap));
            r.setTo(String.valueOf(startTimeMS + (i+1)*gap));
            ranges.add(r);
        }
        aggregation.setRanges(ranges);
        queryParam.put("sRangeAgg", JSON.toJSON(aggregation));
        SearchResultDTO r = searchService.search(new SearchRequestDTO("search-request", queryParam));

        if(r.getAggregationDataMap().containsKey("timeMS")){
            List<SearchResultDTO.AggregationData> list = r.getAggregationDataMap().get("timeMS");
            Map<String, Long> resultMap = new HashMap<>(100);
            list.forEach(k -> resultMap.put(k.getKey(), k.getCount()));
            return Result.buildSuccess(resultMap);
        }
        return Result.buildErr("失败");
    }

    @ApiOperation(value = "慢查询统计", notes = "慢查询统计",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/slowQuery")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceTag", value = "serviceTag",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "startTime", value = "时间格式：yyyy-MM-dd HH:mm:ss",  dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "size", value = "返回的数据条数（默认50）",  dataType = "string", paramType = "query")
    })
    public Result slowQuery(@RequestParam String serviceTag, @RequestParam String startTime,  @RequestParam(required = false) Integer size) {

        size = size == null ? 50 : size;
        Long startTimeMS = getTimeMS(startTime);

        JSONObject queryParam = new JSONObject();
        queryParam.put("iRowSize", size);
        queryParam.put("serviceTag", serviceTag);
        queryParam.put("spendTimeMS", "gte:300");
        queryParam.put("timeMS", "gte:"+startTimeMS);
        SearchResultDTO r = searchService.search(new SearchRequestDTO("search-request", queryParam));
        return Result.buildSuccess(r.getData());
    }


    private Long getGapMills( Integer timeUnit) {
        if(timeUnit == 1){
            // 按天
            return 24*60*60*1000L;
        } else if(timeUnit == 2){
            // 按小时
            return 60*60*1000L;
        } else if(timeUnit == 3){
            // 按分
            return 60*1000L;
        } else if(timeUnit == 4){
            // 按秒
            return 1000L;
        } else {
            return 60*1000L; // 默认按分
        }
    }

    private Long getTimeMS(String time){
        try {
            return DateUtils.parseDate(time, "yyyy-MM-dd HH:mm:ss").getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0L;
    }
}
