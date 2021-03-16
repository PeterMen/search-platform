package com.peter.search.controller;

import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.dto.SearchResultDTO;
import com.peter.search.service.impl.ScrollServiceImpl;
import com.peter.search.util.PropertyUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController("scroll")
@RequestMapping("/scroll")
public class ScrollController {

    @Autowired
    private ScrollServiceImpl scrollService;

    /**
     * scroll接口
     *
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "scroll接口", notes = "scroll接口",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/scrollFirst")
    public SearchResultDTO scrollFirst(@RequestBody SearchRequestDTO<JSONObject> requestParam) {

        try {
            PropertyUtils.SEARCH_CONFIG.set(requestParam.getSearchConfig());
            // step 4: ES 查询
            return scrollService.scrollFirst(requestParam.getServiceTag(), requestParam.getQueryParam());
        } finally {
            PropertyUtils.SEARCH_CONFIG.remove();
        }
    }

    /**
     * scroll接口，请先调用scrollFirst 获取scrollId
     *
     * @author wanghaitao
     * @throws Exception
     */
    @ApiOperation(value = "scrollAfter接口", notes = "scrollAfter接口",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/scrollAfter")
    public SearchResultDTO scrollAfter(@RequestParam String serviceTag, @RequestParam String scrollId) {
        SearchResultDTO searchResultDTO = new SearchResultDTO();
        try {
            return scrollService.scrollAfter(serviceTag, scrollId);
        } catch (IOException e){
            searchResultDTO.setErrMsg(e.getLocalizedMessage());
            searchResultDTO.setStatus(0);
        }
        return searchResultDTO;
    }
}
