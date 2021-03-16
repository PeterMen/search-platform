package com.peter.search.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.MSearchResultDTO;
import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.dto.SuggestRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * ES查询入口
 *
 * @author 王海涛
 * */
@FeignClient(value="search-service", path = "search-service")
public interface SearchServiceApi {

    /**
     * 查询接口
     *
     * @param requestParam 查询参数
     * @return 查询结果
     */
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    SearchResultDTO search(@RequestBody SearchRequestDTO<JSONObject> requestParam);

    /**
     * 批量查询接口
     *
     * @param requestParam 查询参数
     * @return 查询结果
     */
    @PostMapping(value = "/mSearch", consumes = MediaType.APPLICATION_JSON_VALUE)
    MSearchResultDTO mSearch(@RequestBody SearchRequestDTO<JSONArray> requestParam);

    /**
     * 批量查询接口
     *
     * @param serviceTag 业务标识
     * @param searchDSLJson 源生DSL查询语句
     * @throws Exception
     * @return 查询结果
     */
    @PostMapping(value = "/restfulSearch")
    String restfulSearch(@RequestParam(value = "serviceTag", required = true) String serviceTag,
                         @RequestParam(value = "searchDSLJson", required = true) String searchDSLJson) throws Exception ;

    /**
     * 获取提示词接口
     *
     * @author wanghaitao
     * @throws Exception
     */
    @PostMapping(value = "/suggester")
    SearchResultDTO suggester(@RequestBody SuggestRequest suggestRequest);
}