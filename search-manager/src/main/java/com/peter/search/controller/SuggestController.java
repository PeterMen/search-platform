package com.peter.search.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.annotation.SyncDataMQClient;
import com.peter.search.api.SuggestWordApi;
import com.peter.search.dao.IndexDao;
import com.peter.search.dao.SuggestSourceFieldDao;
import com.peter.search.dto.OP_TYPE;
import com.peter.search.dto.Result;
import com.peter.search.dto.SuggestWordDTO;
import com.peter.search.entity.Constant;
import com.peter.search.entity.Index;
import com.peter.search.entity.SuggestSourceField;
import com.peter.search.pojo.Column;
import com.peter.search.pojo.DocData;
import com.peter.search.vo.SuggestFieldVO;
import com.peter.search.vo.SuggestWordVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Api(tags = "联想词管理", position=11)
@RestController(value = "suggest")
@RequestMapping("/suggest")
public class SuggestController {

    private static final Logger logger = LoggerFactory.getLogger(SuggestController.class);
    public static final String ORIGINAL_WORD = "originalWord";
    public static final String TOKENIZED_WORD = "tokenizedWord";

    @Autowired
    private SyncDataMQClient syncDataMQClient;

    @Autowired
    private SuggestSourceFieldDao suggestSourceFieldDao;

    @Autowired
    private IndexDao indexDao;

    @Autowired
    private SuggestWordApi suggestWordApi;

    @ApiOperation(value = "训练提示词", notes = "训练提示词", produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/generalSuggestWord")
    public Result generalSuggestWord(@RequestParam String serviceTag){

        // step1: 查询提示词来源字段
        List<SuggestSourceField> suggestSourceFields = suggestSourceFieldDao.getAllField(serviceTag);
        Assert.notEmpty(suggestSourceFields, "请先关联提示词字段");

        List<String> suggestFields = suggestSourceFields.stream().filter(r -> r.getType()==1).map(r -> r.getFiledName()).collect(Collectors.toList());
        List<String> conditionFields = suggestSourceFields.stream().filter(r -> r.getType()==2).map(r -> r.getFiledName()).collect(Collectors.toList());

        SuggestWordDTO dto = new SuggestWordDTO();
        dto.setConditionFields(conditionFields);
        dto.setSuggestFields(suggestFields);
        dto.setServiceTag(serviceTag);
        suggestWordApi.generalSuggestWord(dto);
        return Result.buildSuccess();
    }

    @ApiOperation(value = "新增提示词字段", notes = "新增提示词字段", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/addSuggestField")
    public Result addSuggestField(@RequestBody SuggestFieldVO suggestFieldVO){

        Assert.isTrue(!suggestSourceFieldDao.existSuggestSourceField(suggestFieldVO.getServiceTag(), suggestFieldVO.getFieldName(), suggestFieldVO.getType()), "字段已存在，不能重复添加");
        suggestSourceFieldDao.addSuggestSourceField(suggestFieldVO.getServiceTag(), suggestFieldVO.getFieldName(), suggestFieldVO.getType());
        return Result.buildSuccess("添加成功");
    }

    @ApiOperation(value = "查询所有已添加的提示词字段", notes = "查询所有已添加的提示词字段", produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/getAllSuggestField")
    public Result getAllSuggestField(@RequestParam String serviceTag){

        return Result.buildSuccess(suggestSourceFieldDao.getAllField(serviceTag));
    }

    @ApiOperation(value = "删除已添加的提示词字段", notes = "删除已添加的提示词字段", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/delSuggestField")
    public Result delSuggestField(@RequestBody SuggestFieldVO suggestFieldVO) {

        suggestSourceFieldDao.deletSuggestSourceField(suggestFieldVO.getServiceTag(), suggestFieldVO.getFieldName(), suggestFieldVO.getType());
        return Result.buildSuccess("删除成功");
    }

    @ApiOperation(value = "查询所有可选择的提示词字段", notes = "查询所有可选择的提示词字段", produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/getAllSuggestSourceField")
    public Result getAllSuggestSourceField(@RequestParam String serviceTag) {

        Index index = indexDao.selectOne(serviceTag);
        List<Column> columns = JSON.parseArray(index.getMapping(), Column.class);
        List<String> sourceFields = parseColumns("", columns);

        return Result.buildSuccess(sourceFields);
    }

    private List<String> parseColumns(String root, List<Column> columns){
        List<String> sourceFields = new ArrayList<>();
        for(Column column : columns){
            if(!CollectionUtils.isEmpty(column.getColumns())){
                sourceFields.addAll(parseColumns(root+column.getColumnName()+".", column.getColumns()));
            } else {
                sourceFields.add(root+column.getColumnName());
            }
        }
        return sourceFields;
    }


    @ApiOperation(value = "新增提示词", notes = "新增提示词", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/addSuggestWord")
    public Result addSuggestWord(@RequestBody SuggestWordVO suggestWordVO){

        Assert.notEmpty(suggestWordVO.getSuggestWord(), "提示词不能为空。");

        List<DocData> docDataList  = new ArrayList<>();
        for(String word : suggestWordVO.getSuggestWord().split(",")){

            JSONObject json = new JSONObject();
            json.put(ORIGINAL_WORD, word);
            json.put(TOKENIZED_WORD, word);
            json.put("serviceTag", suggestWordVO.getServiceTag());
            json.put("type", 2);
            json.putAll(suggestWordVO.getConditionMap());
            DocData docData = new DocData();
            docData.setDocData(json.toJSONString());
            docDataList.add(docData);
        }

        syncDataMQClient.syncData(Constant.SERVICE_TAG_SUGGEST, OP_TYPE.INSERT, docDataList);
        return Result.buildSuccess("添加成功");
    }
}
