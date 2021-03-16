package com.peter.search.controller;

import com.peter.search.datametrics.DataMetricFactory;
import com.peter.search.dto.DeltaImportDataMetrics;
import com.peter.search.dto.FullImportDataMetrics;
import com.peter.search.dto.Result;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.util.Assert;

@Api(tags = "数据统计")
@RestController(value = "dataMetrics")
@RequestMapping("/dataMetrics")
public class DataMetricsController {

    public static final String ERR_MSG = "serviceTag不能为空";

    @GetMapping(value = "getFullImportDataMetricsSelf")
    public FullImportDataMetrics getFullImportDataMetricsSelf(@RequestParam String serviceTag){
        Assert.notEmpty(serviceTag, ERR_MSG);
        return DataMetricFactory.getInstance().getFullDataMetrics(serviceTag);
    }

    @GetMapping(value = "getDeltaImportDataMetricsSelf")
    public DeltaImportDataMetrics getDeltaImportDataMetricsSelf(@RequestParam String serviceTag){
        Assert.notEmpty(serviceTag, ERR_MSG);
        return DataMetricFactory.getInstance().getDeltaDataMetrics(serviceTag);
    }

    @GetMapping(value = "clearDeltaImportDataMetricsSelf")
    public Result clearDeltaImportDataMetricsSelf(@RequestParam String serviceTag){
        Assert.notEmpty(serviceTag, ERR_MSG);
        DataMetricFactory.getInstance().getDeltaDataMetrics(serviceTag).syncStart();
        return Result.buildSuccess();
    }
}
