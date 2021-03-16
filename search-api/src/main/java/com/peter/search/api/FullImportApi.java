package com.peter.search.api;

import com.peter.search.dto.FullImportDTO;
import com.peter.search.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(value="search-service", path = "search-service")
public interface FullImportApi {

    @PostMapping(value = "/indexUpdate/fullImportData", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Result fullImportData(@RequestBody FullImportDTO param);
}
