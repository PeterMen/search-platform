package com.peter.search.api;

import com.peter.search.dto.DeltaImportDataMetrics;
import com.peter.search.dto.FullImportDataMetrics;
import com.peter.search.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value="search-service", path = "search-service")
public interface DataMetricsApi {

    @GetMapping(value = "getFullImportDataMetricsSelf")
    FullImportDataMetrics getFullImportDataMetricsSelf(@RequestParam String serviceTag);

    @GetMapping(value = "getDeltaImportDataMetricsSelf")
    DeltaImportDataMetrics getDeltaImportDataMetricsSelf(@RequestParam String serviceTag);

    @GetMapping(value = "clearDeltaImportDataMetricsSelf")
    Result clearDeltaImportDataMetricsSelf(@RequestParam String serviceTag);
}
