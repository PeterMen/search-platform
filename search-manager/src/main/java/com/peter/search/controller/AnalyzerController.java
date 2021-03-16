package com.peter.search.controller;

import com.peter.search.dao.IndexAnalyzerDao;
import com.peter.search.dto.Result;
import com.peter.search.entity.IndexAnalyzer;
import com.peter.search.vo.AnalyzerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.util.Assert;

@Api(tags = "analyzer分析器管理")
@RestController(value = "analyzer")
@RequestMapping("/analyzer")
public class AnalyzerController {

    @Autowired
    private IndexAnalyzerDao indexAnalyzerDao;

    @ApiOperation(value = "添加分析器", notes = "添加分析器", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/addAnalyzer")
    public Result addAnalyzer(@RequestBody AnalyzerVO analyzer) {

        IndexAnalyzer ia = new IndexAnalyzer();
        BeanUtils.copyProperties(analyzer, ia);
        // 重复添加检查
        Assert.isTrue(!indexAnalyzerDao.exist(ia), "不可重复添加");

        // 添加
        indexAnalyzerDao.addAnalyzer(ia);
        return Result.buildSuccess("analyzer添加成功。");
    }

    @ApiOperation(value = "查询分析器", notes = "查询分析器", produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/getAllAnalyzer")
    public Result getAllAnalyzer(@RequestParam String serviceTag) {

        Assert.notEmpty(serviceTag, "serviceTag不能为空");

        return Result.buildSuccess(indexAnalyzerDao.getAllAnalyzer(serviceTag));
    }
}
