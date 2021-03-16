package com.peter.search.controller;

import com.peter.search.dao.DictionaryDao;
import com.peter.search.dto.Result;
import com.peter.search.entity.Dictionary;
import com.peter.search.util.FileUtil;
import com.peter.search.vo.DictionaryVO;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.util.Assert;

@Api(tags = "词典管理", position=11)
@RestController(value = "dictionary")
@RequestMapping("/dictionary")
public class DictionaryController {

    @Autowired
    private Environment env;

    @Autowired
    private DictionaryDao dictionaryDao;

    @ApiOperation(value = "上传字典文件", notes = "上传字典文件", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/uploadDictionary")
    public Result uploadDictionary(@RequestPart(value = "mediaFile") MultipartFile mediaFile) {
        //1.校验文件名，文件格式，文件数据是否为空
        String url = FileUtil.saveMultipartFile(mediaFile, env());
        return Result.buildSuccess(ImmutableMap.of("url", url));
    }

    @ApiOperation(value = "添加字典文件", notes = "添加字典文件", produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping(value = "/addDictionary")
    public Result addDictionary(@RequestBody DictionaryVO dictionary) {

        Assert.notEmpty(dictionary.getServiceTag(), "serviceTag不能为空");
        Assert.notNull(dictionary.getIsLocalFile(), "isLocalFile不能为空");
        Assert.notNull(dictionary.getType(), "type不能为空");
        Assert.notEmpty(dictionary.getDicPath(), "dicPath不能为空");
        Assert.notEmpty(dictionary.getDicName(), "dicName不能为空");

        // 校验重复添加
        Assert.isTrue(!dictionaryDao.exist(dictionary.getServiceTag(), dictionary.getDicPath()), "不能重复添加");

        // 保存
        Dictionary d = new Dictionary();
        BeanUtils.copyProperties(dictionary, d);
        d.setIsLocalFile(dictionary.getIsLocalFile());
        dictionaryDao.addDictionary(d);
        return Result.buildSuccess("词典保存成功。");
    }

    @ApiOperation(value = "查询字典文件", notes = "查询字典文件", produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/getAllDictionary")
    public Result getAllDictionary(@RequestParam String serviceTag, @RequestParam(required = false) Integer type) {

        Assert.notEmpty(serviceTag, "serviceTag不能为空");

        return Result.buildSuccess(dictionaryDao.getDictionaries(serviceTag, type));
    }

    @ApiOperation(value = "删除字典文件", notes = "删除字典文件", produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping(value = "/deleteDictionary")
    public Result deleteDictionary(@RequestParam Long dictionaryId) {

        Assert.notNull(dictionaryId, "dictionaryId不能为空");
        Dictionary dictionary = dictionaryDao.getDictionaryById(dictionaryId);
        // 上传文件
        if(!dictionary.getIsLocalFile()){

            // 删除远程字典文件
            FileUtil.deleteFile(dictionary.getDicPath(), env());
        }

        // 删除数据
        dictionaryDao.delDictionary(dictionaryId);
        return Result.buildSuccess("删除成功！");
    }

    private String env(){
        return env.getProperty("dic.upload.path", "sit");
    }

}
