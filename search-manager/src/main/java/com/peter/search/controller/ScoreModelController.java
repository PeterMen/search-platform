package com.peter.search.controller;


import com.peter.search.dao.ScoreModelDao;
import com.peter.search.dto.Result;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.vo.ScoreModelVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.elasticsearch.action.admin.cluster.storedscripts.DeleteStoredScriptRequest;
import org.elasticsearch.action.admin.cluster.storedscripts.PutStoredScriptRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.util.Assert;

import java.io.IOException;

@RestController
@RequestMapping("/score")
@Api(tags = "二次排序", position=6)
public class ScoreModelController {

    @Autowired
    ESClientFactory esClientFactory;

    @Autowired
    ScoreModelDao scoreModelDao;

    @ApiOperation(value = "创建二次排序模型", notes = "创建二次排序模型",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping("/createORUpdateScoreModel")
    @Transactional(rollbackFor = Exception.class)
    public Result createORUpdateScoreModel(@RequestBody ScoreModelVO modelVO) throws IOException {

        Assert.notEmpty(modelVO.getServiceTag(), "serviceTag不能为空");
        Assert.notEmpty(modelVO.getModelContent(), "model不能为空");
        Assert.notEmpty(modelVO.getName(), "name不能为空");

        Long scriptId;
        if(modelVO.getModelId() != null){
            Assert.notNull(scoreModelDao.getScoreModelByID(modelVO.getModelId()), "model不存在");
            // update
            scoreModelDao.updateModel(modelVO.getModelId(), modelVO.getName(), modelVO.getModelContent());
            scriptId = modelVO.getModelId();
        } else {
            // insert
            scriptId = scoreModelDao.createModel(modelVO.getServiceTag(), modelVO.getName(), modelVO.getModelContent());
        }

        RestHighLevelClient client = esClientFactory.getHighLevelClient(modelVO.getServiceTag());
        PutStoredScriptRequest storedScriptRequest = new PutStoredScriptRequest();
        storedScriptRequest.id(scriptId.toString());
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("script");
            {
                builder.field("lang", "painless");
                builder.field("source", modelVO.getModelContent());
            }
            builder.endObject();
        }
        builder.endObject();
        storedScriptRequest.content(BytesReference.bytes(builder), XContentType.JSON);

        client.putScript(storedScriptRequest, RequestOptions.DEFAULT);
        return Result.buildSuccess("保存成功");
    }

    @ApiOperation(value = "删除排序模型", notes = "删除排序模型",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/deleteScoreModel")
    @Transactional(rollbackFor = Exception.class)
    public Result createORUpdateScoreModel(@RequestParam String serviceTag, @RequestParam Long modelId) throws IOException {

        Assert.notNull(scoreModelDao.getScoreModelByID(modelId), "model不存在");

        RestHighLevelClient client = esClientFactory.getHighLevelClient(serviceTag);
        DeleteStoredScriptRequest deleteStoredScriptRequest = new DeleteStoredScriptRequest(modelId.toString());
        client.deleteScript(deleteStoredScriptRequest, RequestOptions.DEFAULT);

        scoreModelDao.delScoreModel(modelId);
        return Result.buildSuccess("删除成功");
    }

    @ApiOperation(value = "查看所有排序模型", notes = "查看所有排序模型",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/getAllScoreModel")
    public Result getAllScoreModel(@RequestParam String serviceTag) throws IOException {
        return Result.buildSuccess(scoreModelDao.getAllScoreModel(serviceTag));
    }



}
