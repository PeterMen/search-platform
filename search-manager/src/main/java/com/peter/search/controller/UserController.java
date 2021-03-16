package com.peter.search.controller;

import com.peter.search.dao.ServiceTagDao;
import com.peter.search.dao.ServiceTagUserDao;
import com.peter.search.dto.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.util.Assert;

@RestController
@RequestMapping("/user")
@Api(tags = "用户权限管理", position=6)
public class UserController {

    @Autowired
    private ServiceTagUserDao serviceTagUserDao;
    @Autowired
    private ServiceTagDao serviceTagManageDao;

    @ApiOperation(value = "查询用户", notes = "查询用户",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/getUser")
    public Result getUser(@RequestParam String serviceTag){

        Assert.notEmpty(serviceTag, "serviceTag不能为空");
        return Result.buildSuccess(serviceTagUserDao.getUserByServiceTag(serviceTag));
    }

    @ApiOperation(value = "添加用户", notes = "添加用户",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/addUser")
    public Result addUser(@RequestParam String serviceTag, @RequestParam String user){

        Assert.notEmpty(user, "用户名不能为空");
        Assert.notEmpty(serviceTag, "serviceTag不能为空");
        Assert.notNull(serviceTagManageDao.getServiceTag(serviceTag), "serviceTag不存在。");
        Assert.isTrue(!serviceTagUserDao.isExist(serviceTag, user), "不能重复添加。");

        serviceTagUserDao.insert(serviceTag, user);
        return Result.buildSuccess("添加成功。");
    }

    @ApiOperation(value = "删除用户", notes = "删除用户",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/deleteUser")
    public Result deleteUser(@RequestParam String serviceTag, @RequestParam String user){

        Assert.notEmpty(user, "用户名不能为空");
        Assert.notEmpty(serviceTag, "serviceTag不能为空");

        serviceTagUserDao.deleteByUser(serviceTag, user);
        return Result.buildSuccess("删除成功。");
    }

    @ApiOperation(value = "添加超级管理员", notes = "添加超级管理员",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/addAdmin")
    public Result addAdmin(@RequestParam String user){

        Assert.notEmpty(user, "用户名不能为空");
        Assert.isTrue(!serviceTagUserDao.isAdminExist(user), user+"已经是管理员了。");

        serviceTagUserDao.addAdmin(user);
        return Result.buildSuccess("添加成功。");
    }
}
