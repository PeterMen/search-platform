package com.peter.search.controller;

import com.peter.search.dao.DBMsgDao;
import com.peter.search.dto.Result;
import com.peter.search.entity.FailedMsg;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@Api(tags = "失败消息补偿", position=5)
@RestController
@RequestMapping("/failedMsg")
public class FailedMsgProcessController {

    @Autowired
    DBMsgDao dbMsgDao;

    @ApiOperation(value = "统计请求异常数", notes = "统计请求异常数",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/getFailedMsg")
    public Result getFailedMsg(@RequestParam String serviceTag, @RequestParam Integer currentPage, @RequestParam Integer pageSize) {

        currentPage = currentPage == null ? 1 : currentPage;
        pageSize = pageSize == null ? 20 : pageSize;

        return  Result.buildSuccess(dbMsgDao.getFailedMsgList(serviceTag,(currentPage-1)*pageSize, pageSize));

    }

    @ApiOperation(value = "统计请求异常数", notes = "统计请求异常数",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/updateFailedMsg")
    public Result getFailedMsg(@RequestBody FailedMsg failedMsg) {
        dbMsgDao.updateFailedMsg(failedMsg);

        return Result.buildSuccess();
    }
}
