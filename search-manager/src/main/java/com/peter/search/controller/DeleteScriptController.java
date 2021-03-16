package com.peter.search.controller;

import com.peter.search.api.UpdateServiceApi;
import com.peter.search.dao.DataDeleteTriggerDao;
import com.peter.search.dto.Result;
import com.peter.search.entity.DeleteTrigger;
import com.peter.search.util.DeleteJsonUtil;
import com.peter.search.vo.DeleteTriggerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.util.Assert;

import java.io.IOException;

@Api(tags = "数据删除脚本")
@RestController
@RequestMapping("/deleteScript")
public class DeleteScriptController {

    @Autowired
    private DataDeleteTriggerDao deleteTriggerDao;
    @Autowired
    private UpdateServiceApi updateServiceApi;

    @ApiOperation(value = "查询定时数据删除任务", notes = "查询定时数据删除任务",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/getIndexDataDeleteSetting")
    public Result getIndexDataDeleteSetting(String serviceTag) throws IOException {

        Assert.notEmpty(serviceTag, "serviceTag不能为空。");
        return Result.buildSuccess(deleteTriggerDao.getTrigger(serviceTag));
    }

    @ApiOperation(value = "删除定时数据删除任务", notes = "删除定时数据删除任务",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/deleteIndexDataDeleteSetting")
    public Result deleteIndexDataDeleteSetting(Long id) throws IOException {

        Assert.notNull(id, "id不能为空。");
        deleteTriggerDao.deleteTriggerById(id);
        return Result.buildSuccess("删除成功");
    }

    @ApiOperation(value = "创建定时数据删除任务", notes = "创建定时数据删除任务",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping("/createIndexDataDeleteSetting")
    public Result createIndexDataDeleteSetting(@Validated @RequestBody DeleteTriggerVO triggerVO) throws IOException {

        Assert.isTrue(triggerVO.getTriggerType() == 1 || triggerVO.getTriggerType() == 2, "triggerType不正确");

        DeleteTrigger trigger = new DeleteTrigger();
        BeanUtils.copyProperties(triggerVO, trigger);
        deleteTriggerDao.insert(trigger);
        return Result.buildSuccess("创建成功");
    }

    @ApiOperation(value = "编辑定时数据删除任务", notes = "编辑定时数据删除任务",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping("/editIndexDataDeleteSetting")
    public Result editIndexDataDeleteSetting(@Validated @RequestBody DeleteTriggerVO triggerVO) throws IOException {
        Assert.notNull(deleteTriggerDao.getTrigger(triggerVO.getServiceTag()), "IndexDataDeleteSetting未创建。");
        Assert.isTrue(triggerVO.getTriggerType() == 1 || triggerVO.getTriggerType() == 2, "triggerType不正确");
        Assert.notNull(deleteTriggerDao.getTrigger(triggerVO.getServiceTag()), "IndexDataDeleteSetting未创建。");

        DeleteTrigger trigger = new DeleteTrigger();
        BeanUtils.copyProperties(triggerVO, trigger);
        deleteTriggerDao.updateTrigger(trigger);
        return Result.buildSuccess("编辑成功");
    }

    @ApiOperation(value = "脚本测试", notes = "脚本测试",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/scriptTest")
    public Result scriptTest(@RequestParam(name = "serviceTag") String serviceTag,
                             @RequestParam(name = "deleteJson") String deleteJson) {

        updateServiceApi.deleteByQuery(serviceTag, DeleteJsonUtil.handleToken(deleteJson));
        return Result.buildSuccess("删除成功,请验证数据！");
    }
}
