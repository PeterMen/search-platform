package com.peter.search.controller;

import com.peter.search.dao.ServiceTagDao;
import com.peter.search.dao.ServiceTagUserDao;
import com.peter.search.dto.Result;
import com.peter.search.entity.ServiceTag;
import com.peter.search.vo.ServiceTagVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.util.Assert;

import javax.servlet.http.HttpServletRequest;

import static com.peter.search.util.Constant.RELOAD_SERVICE_TAG;

@Api(tags = "serviceTag管理接口", position=6)
@RestController
@RequestMapping("/serviceTagManage")
public class ServiceTagManageController {

    @Autowired
    private ServiceTagDao serviceTagManageDao;

    @Autowired
    private ServiceTagUserDao serviceTagUserDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IndexManageController indexManageController;

    @ApiOperation(value = "查询serviceTag", notes = "查询serviceTag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/getServiceTag")
    public Result getServiceTag(@RequestParam String serviceTag){

        Assert.notEmpty(serviceTag, "serviceTag不能为空。");

        return Result.buildSuccess(serviceTagManageDao.getServiceTag(serviceTag));
    }

    @ApiOperation(value = "查询所有serviceTag", notes = "查询所有serviceTag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/getAllServiceTag")
    public Result getAllServiceTag(HttpServletRequest request){

        String userId = request.getHeader("userId");
        Assert.notEmpty(userId, "用户未登陆。");

        return Result.buildSuccess(serviceTagUserDao.getAllServiceTag(userId));
    }

    /**
     * 创建serviceTag
     * */
    @ApiOperation(value = "创建serviceTag", notes = "创建serviceTag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping("/createServiceTag")
    public Result createServiceTag(HttpServletRequest request, @RequestBody ServiceTagVO serviceTagVO){

        String userId = request.getHeader("userId");
        Assert.notEmpty(userId, "用户未登陆。");

        Assert.notEmpty(serviceTagVO.getServiceTag(), "serviceTag不能为空。");
        Assert.isNull(serviceTagManageDao.getServiceTag(serviceTagVO.getServiceTag()), "serviceTag已存在，不能重复创建。");

        ServiceTag serviceTag = new ServiceTag();
        BeanUtils.copyProperties(serviceTagVO, serviceTag);
        // indexName, typeName一律设置为serviceTag的小写形式
        serviceTag.setIndexAlias(serviceTagVO.getServiceTag().toLowerCase());
        serviceTag.setTypeName(serviceTagVO.getServiceTag().toLowerCase());
        serviceTagManageDao.createServiceTag(serviceTag);
        serviceTagUserDao.insert(serviceTagVO.getServiceTag(), userId);

        stringRedisTemplate.convertAndSend(RELOAD_SERVICE_TAG, serviceTag.getServiceTag());
        return Result.buildSuccess("serviceTag创建成功。");
    }

    /**
     * 编辑serviceTag
     * */
    @ApiOperation(value = "编辑serviceTag", notes = "编辑serviceTag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping("/editServiceTag")
    public Result editServiceTag(@RequestBody ServiceTagVO serviceTagVO){

        // 校验
        Assert.notEmpty(serviceTagVO.getServiceTag(), "serviceTag不能为空。");
        Assert.notNull(serviceTagManageDao.getServiceTag(serviceTagVO.getServiceTag()), "serviceTag不存在。");

        // 保存DB
        ServiceTag serviceTag = new ServiceTag();
        BeanUtils.copyProperties(serviceTagVO, serviceTag);
        serviceTagManageDao.updateServiceTag(serviceTag);

        stringRedisTemplate.convertAndSend(RELOAD_SERVICE_TAG, serviceTag.getServiceTag());
        return Result.buildSuccess("serviceTag编辑成功。");
    }

    /**
     * 删除serviceTag将会删除serviceTag相关的index和es上的所有数据
     * */
    @ApiOperation(value = "删除serviceTag", notes = "删除serviceTag",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/deleteServiceTag")
    public Result deleteServiceTag(@RequestParam String serviceTag) {

        Assert.notEmpty(serviceTag, "serviceTag不能为空");

        try {
            // step1: 删除索引
            indexManageController.deleteIndexSettings(serviceTag);

            // step2: 删除serviceTag
            serviceTagManageDao.deleteServiceTag(serviceTag);
            serviceTagUserDao.deleteByServiceTag(serviceTag);

            return Result.buildSuccess("删除成功。");
        } catch (Exception e){
            return Result.buildSuccess("删除失败" + e.getMessage());
        }
    }
}
