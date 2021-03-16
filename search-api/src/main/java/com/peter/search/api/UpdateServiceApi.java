package com.peter.search.api;

import com.peter.search.dto.Result;
import com.peter.search.dto.UpdateRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * ES索引更新
 *
 * @author 王海涛
 * */
@FeignClient(value = "search-service", path = "search-service")
public interface UpdateServiceApi {

    /**
     * update接口
     *
     * @param serviceTag 业务标识
     * @param docId ES文档id
     * @param updateJson 源生DSL update语句
     * @throws Exception
     * @return 更新结果
     */
    @RequestMapping(value = "/restfulUpdate", method = RequestMethod.GET)
    String restfulUpdate(@RequestParam(name = "serviceTag") String serviceTag,
                         @RequestParam(name = "docId") String docId,
                         @RequestParam(name = "updateJson") String updateJson) throws Exception;

    /**
     * 用脚本执行更新
     *
     * @param serviceTag
     * @param docId
     * @param paramMap
     * @param scriptId
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/updateByScript", method = RequestMethod.GET)
    String updateByScript(@RequestParam(name = "serviceTag") String serviceTag,
                          @RequestParam(name = "docId") String docId,
                          @RequestParam(name = "paramMap") Map<String, Object> paramMap,
                          @RequestParam(name = "scriptId") String scriptId) throws Exception;

    /**
     * 删除接口
     *
     * @param serviceTag 业务标识
     * @param docId ES文档id
     * @throws Exception
     * @return 删除结果
     */
    @RequestMapping(value = "/deleteByDocId", method = RequestMethod.GET)
    Result deleteByDocId(@RequestParam(name = "serviceTag") String serviceTag,
                         @RequestParam(name = "docId") String docId) throws Exception;

    /**
     * 删除接口
     *
     * @param serviceTag 业务标识
     * @param deleteJson 源生DSL update语句
     * @throws Exception
     * @return 删除结果
     */
    @RequestMapping(value = "/deleteByQuery", method = RequestMethod.GET)
    Result deleteByQuery(@RequestParam(name = "serviceTag") String serviceTag,
                         @RequestParam(name = "deleteJson", required = false) String deleteJson);
    /**
     * 索引增量更新消费队列
     * */
    @RequestMapping(value = "/deltaImportData", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    Result deltaImportData(@RequestBody UpdateRequestDTO updateRequestDTO);
}
