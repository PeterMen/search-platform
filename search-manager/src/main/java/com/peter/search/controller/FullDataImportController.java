package com.peter.search.controller;

import com.peter.search.api.FullImportApi;
import com.peter.search.dao.IndexDao;
import com.peter.search.dto.FullImportDTO;
import com.peter.search.dto.Result;
import com.peter.search.service.impl.IndexCreateServiceImpl;
import com.peter.search.service.impl.ServiceTagInfoImpl;
import com.peter.search.vo.FullImportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.util.Assert;

import java.io.IOException;

import static com.peter.search.util.Constant.FULL_IMPORT_LOCK;

/**
 * 索引数据全量更新
 *
 * @author 王海涛
 * */
@Api(tags = "数据全量更新接口")
@RestController(value = "fullDataImport")
@RequestMapping("/indexUpdate")
public class FullDataImportController {

    @Autowired
    private IndexDao indexDao;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private FullImportApi fullImportApi;
    @Autowired
    private ServiceTagInfoImpl serviceTagInfo;
    @Autowired
    private IndexCreateServiceImpl indexCreator;

    /**
     * 索引全量更新
     *
     * */
    @ApiOperation(value = "索引全量更新", notes = "索引全量更新",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "fullImportData", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Result fullImportData(@RequestBody FullImportVO param) throws IOException {
        String serviceTag = param.getServiceTag();
        String fullImportUrl = param.getFullImportUrl();
        if(StringUtils.isEmpty(fullImportUrl)){
            fullImportUrl = serviceTagInfo.getDataSourceURL(serviceTag);
            param.setFullImportUrl(fullImportUrl);
        }

        Assert.notEmpty(serviceTag, "serviceTag不能为空");
        Assert.notEmpty(fullImportUrl, "数据源地址未设置");

        // step0: indexName检查
        Assert.notNull(indexDao.selectOne(serviceTag), "index未创建，请先创建index.");

        // step1:幂等性检查
        RLock lock = redissonClient.getLock(FULL_IMPORT_LOCK+serviceTag);
        if(lock.isLocked()){
            return Result.buildErr("全量更新进行中，请稍后再试!");
        }

        FullImportDTO importDTO = new FullImportDTO();
        importDTO.setPageSize(param.getPageSize());
        importDTO.setExtraParams(param.getExtraParams());
        importDTO.setFullImportUrl(param.getFullImportUrl());
        // 当条件为空时，需要重建所有index数据，创建新的index，如果带条件同步，则数据同步到原index中
        importDTO.setNewIndexName(MapUtils.isEmpty(param.getExtraParams()) ? indexCreator.createNewIndexCopy(serviceTag) : null);
        importDTO.setServiceTag(serviceTag);
        importDTO.setUseFeign(param.getUseFeign());
        fullImportApi.fullImportData(importDTO);

        return Result.buildSuccess("索引同步开始....");
    }
}
