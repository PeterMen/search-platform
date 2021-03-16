package com.peter.search.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.datametrics.DataMetricFactory;
import com.peter.search.datametrics.SearchRequestDataMetrics;
import com.peter.search.pojo.CheckResult;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.util.PropertyUtils;
import com.peter.search.dto.MSearchResultDTO;
import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.dto.SuggestRequest;
import com.peter.search.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;


/**
 *  搜索
 *
 * @author 七星
 * @date 2018年02月02号
 */
@Api(tags = "搜索接口", position=10)
@RestController(value = "esSearchService")
public class SelectController{

    private static final Logger logger = LoggerFactory.getLogger(SelectController.class);
    public static final String SEARCH_USE_CACHE = "search.useCache";

    @Autowired
    private SuggestService suggestService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private MSearchService mSearchService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ParamCheckService paramCheckService;

    @Autowired
    private PropertyUtils properties;

    @Autowired
    private ESClientFactory esClientFactory;

    /**
     * 查询接口
     * 
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "搜索接口", notes = "搜索接口",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/search")
    public SearchResultDTO search(@RequestBody SearchRequestDTO<JSONObject> requestParam) {

        // 请求开始处理时间
        long startTime = System.currentTimeMillis();

        SearchResultDTO searchResult = new SearchResultDTO(SearchResultDTO.STATUS_SUCCESS);
        if(logger.isDebugEnabled()){
            logger.debug("--搜索参数： {}", requestParam.toString());
        }

        try{

            // step 1: 参数校验
            CheckResult checkResult = paramCheckService.check(requestParam);
            if(!checkResult.getCheckStatus()){
                searchResult.setStatus(SearchResultDTO.STATUS_FAILED);
                searchResult.setErrMsg(checkResult.getErrMsg());
                // 统计请求信息
                logSearchRequest(requestParam, SearchResultDTO.STATUS_FAILED, 0L);
                return searchResult;
            }

            // step 2: get from cache
            if(requestParam.getUseCache() && Boolean.valueOf(properties.getProperty(SEARCH_USE_CACHE))){
                searchResult = cacheService.getFromCache(requestParam, SearchResultDTO.class);
                if(searchResult != null){
                    if(logger.isDebugEnabled()){
                        logger.debug("----I'm cache you---");
                    }
                    return searchResult;
                }
            }

            try {
                PropertyUtils.SEARCH_CONFIG.set(requestParam.getSearchConfig());

                // step 4: ES 查询
                searchResult = searchService.search(requestParam.getServiceTag(), requestParam.getQueryParam());
            } finally {
                PropertyUtils.SEARCH_CONFIG.remove();
            }

            // step 5: update cache
            if(requestParam.getUseCache() && Boolean.valueOf(properties.getProperty(SEARCH_USE_CACHE))){
                cacheService.saveToCache(requestParam, searchResult);
            }

        }catch (Exception e){
            if(searchResult == null){
                searchResult = new SearchResultDTO();
            }
            searchResult.setStatus(SearchResultDTO.STATUS_FAILED);
            searchResult.setErrMsg(e.getMessage());
            logger.error("ES 查询失败：", e);
        }

        // 请求处理结束时间
        Long tookMillis = System.currentTimeMillis() - startTime;
        searchResult.setTookMillis(tookMillis);

        if(logger.isDebugEnabled()){
            logger.debug("--查询结果--{}", JSON.toJSONString(searchResult) );
        }

        // 统计请求信息
        logSearchRequest(requestParam, searchResult.getStatus(), tookMillis);

        return searchResult;
    }


    /**
     * 批量查询接口
     *
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "批量搜索接口", notes = "批量搜索接口",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/mSearch")
    public MSearchResultDTO mSearch(@RequestBody SearchRequestDTO<JSONArray> requestParam) {

        // 请求开始处理时间
        long startTime = System.currentTimeMillis();

        MSearchResultDTO searchResult = new MSearchResultDTO(SearchResultDTO.STATUS_SUCCESS);
        if(logger.isDebugEnabled()){
            logger.debug("--搜索参数： {}", JSON.toJSONString(requestParam));
        }

        try{

            // step 1: 参数校验
            CheckResult checkResult = paramCheckService.check(requestParam);
            if(!checkResult.getCheckStatus()){
                searchResult.setStatus(SearchResultDTO.STATUS_FAILED);
                searchResult.setErrMsg(checkResult.getErrMsg());
                return searchResult;
            }

            // step 2: get from cache
            if(requestParam.getUseCache() && Boolean.valueOf(properties.getProperty(SEARCH_USE_CACHE))){
                searchResult = cacheService.getFromCache(requestParam, MSearchResultDTO.class);
                if(searchResult != null){
                    if(logger.isDebugEnabled()){
                        logger.debug("----I'm cache you---");
                    }
                    return searchResult;
                }
            }

            try{
                // setp 3: 查询参数适配
                PropertyUtils.SEARCH_CONFIG.set(requestParam.getSearchConfig());

                // setp 4: 批量ES查询
                searchResult = mSearchService.mSearch(requestParam.getServiceTag(), requestParam.getQueryParam());
            }finally {
                PropertyUtils.SEARCH_CONFIG.remove();
            }


            // step 5: update cache
            if(requestParam.getUseCache() && Boolean.valueOf(properties.getProperty(SEARCH_USE_CACHE))){
                cacheService.saveToCache(requestParam, searchResult);
            }

        }catch (Exception e){
            if(searchResult == null){
                searchResult = new MSearchResultDTO();
            }
            searchResult.setStatus(SearchResultDTO.STATUS_FAILED);
            searchResult.setErrMsg(e.getMessage());
            logger.error("ES 查询失败", e);
        }

        // 请求处理结束时间
        searchResult.setServiceRespTimeMillis(System.currentTimeMillis() - startTime);

        // 统计请求信息
        logSearchRequest(requestParam, searchResult.getStatus(), System.currentTimeMillis() - startTime);

        if(logger.isDebugEnabled()){
            logger.debug("--查询结果--{}", JSON.toJSONString(searchResult) );
        }

        return searchResult;
    }

    /**
     * 批量查询接口
     *
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "源生DSL语句查询", notes = "源生DSL语句查询",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "serviceTag", value = "业务标识：APP、H5、PC", required = true, dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "searchDSLJson", value = "es dsl查询语句", required = true, dataType = "string", paramType = "form"),
    })
    @PostMapping(value = "/restfulSearch")
    public String restfulSearch(@RequestParam(value = "serviceTag", required = true)String serviceTag,
                                @RequestParam(value = "searchDSLJson", required = true)String searchDSLJson) throws Exception{

        String index = properties.getIndexAlias(serviceTag);
        String type = properties.getTypeName(serviceTag);
        StringEntity stringEntity = new StringEntity(searchDSLJson);
        stringEntity.setContentEncoding("UTF-8");
        stringEntity.setContentType("application/json");
        Response response = esClientFactory.getRestClient(serviceTag)
                .performRequest("GET", index +"/"+ type + "/_search", new HashMap<>(4), stringEntity);

        return EntityUtils.toString(response.getEntity());
    }

    /**
     * 获取提示词接口
     *
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "提示词接口", notes = "提示词接口",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/suggester")
    public SearchResultDTO suggester(@RequestBody SuggestRequest suggestRequest) {

        SearchResultDTO suggestResult = new SearchResultDTO(1);

        try{

            suggestResult = suggestService.suggest(suggestRequest.getServiceTag(), suggestRequest);

        }catch (Exception e){
            if(suggestResult == null){
                suggestResult = new SearchResultDTO(0);
            }
            suggestResult.setStatus(SearchResultDTO.STATUS_FAILED);
            suggestResult.setErrMsg(e.getMessage());
            logger.error("ES 查询失败", e);
        }
        return suggestResult;
    }

    private void logSearchRequest(SearchRequestDTO requestParam, Integer status, Long tookMillis) {
        if(status == SearchResultDTO.STATUS_SUCCESS){
            Integer slowLimit = Integer.valueOf(properties.getProperty(SearchRequestDataMetrics.SLOW_QUERY_TIME, "500"));
            DataMetricFactory.getInstance()
                    .getSearchRequestDataMetrics(Thread.currentThread().getName())
                    .add(new SearchRequestDataMetrics(requestParam.getServiceTag(),
                            tookMillis,
                            false, tookMillis > slowLimit ? requestParam : null));
        } else {
            DataMetricFactory.getInstance()
                    .getSearchRequestDataMetrics(Thread.currentThread().getName())
                    .add(new SearchRequestDataMetrics(requestParam.getServiceTag(),
                            tookMillis, true,  requestParam));
        }
    }
}
