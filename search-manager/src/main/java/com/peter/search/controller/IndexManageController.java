package com.peter.search.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.dao.DataDeleteTriggerDao;
import com.peter.search.dao.IndexDao;
import com.peter.search.dao.ServiceTagDao;
import com.peter.search.dao.ServiceTagIndexDao;
import com.peter.search.dto.Result;
import com.peter.search.entity.Index;
import com.peter.search.pojo.Column;
import com.peter.search.service.client.ESClientFactory;
import com.peter.search.service.impl.IndexCreateServiceImpl;
import com.peter.search.pojo.DataType;
import com.peter.search.service.impl.ServiceTagInfoImpl;
import com.peter.search.vo.IndexVO;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tk.mybatis.mapper.util.Assert;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static com.peter.search.util.Constant.*;

@Api(tags = "索引管理接口", position=5)
@RestController
@RequestMapping("/indexManage")
@Slf4j
public class IndexManageController {

    @Autowired
    private IndexDao indexDao;
    @Autowired
    private ServiceTagDao serviceTagDao;
    @Autowired
    private IndexCreateServiceImpl indexCreator;
    @Autowired
    private ServiceTagIndexDao serviceTagIndexDao;
    @Autowired
    private DataDeleteTriggerDao deleteTriggerDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ESClientFactory esClientFactory;
    @Autowired
    private ServiceTagInfoImpl serviceTagInfo;

    @ApiOperation(value = "查询index信息", notes = "查询index信息",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/getIndexSettings")
    public Result getIndexSettings(@RequestParam String serviceTag){

        Assert.notEmpty(serviceTag, "serviceTag不能为空。");

        Index index = indexDao.selectOne(serviceTag);
        if(index != null ){
            IndexVO indexVO = new IndexVO();
            indexVO.setReplicationNum(index.getReplicationNum());
            indexVO.setServiceTag(serviceTag);
            indexVO.setShardingNum(index.getShardingNum());
            indexVO.setColumns(JSON.parseArray(index.getMapping(), Column.class));
            return Result.buildSuccess(indexVO);
        } else {
            return Result.buildSuccess("index未创建");
        }
    }

    @ApiOperation(value = "删除index信息", notes = "删除index信息",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/deleteIndexSettings")
    @Transactional(rollbackFor = Exception.class)
    public Result deleteIndexSettings(@RequestParam String serviceTag){

        Assert.notEmpty(serviceTag, "serviceTag不能为空。");

        try{
            // step1: 删除索引数据
            List<String> indexNameList = serviceTagIndexDao.deleteAll(serviceTag);
            for(String indexName : indexNameList){
                // 删除ES索引
                indexCreator.indexDelete(serviceTag, indexName);
            }

            // step2: 删除index
            indexDao.deleteByServiceTag(serviceTag);

            // step3: 删除定时任务
            deleteTriggerDao.deleteTrigger(serviceTag);

            return Result.buildSuccess("删除成功。");
        } catch (Exception e){
            return Result.buildSuccess("删除失败："+e.getMessage());
        }
    }

    /**
     * 创建index
     * */
    @ApiOperation(value = "创建或编辑index结构", notes = "创建或编辑index结构",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PostMapping("/createOrEditIndex")
    public Result createOrEditIndex(@RequestBody IndexVO indexVO) throws IOException {

        String serviceTag = indexVO.getServiceTag();
        Assert.notEmpty(serviceTag, "serviceTag不能为空");
        Assert.notNull(serviceTagDao.getServiceTag(serviceTag), "serviceTag未创建，请先创建serviceTag.");
        String indexAlias = serviceTagInfo.getIndexAlias(serviceTag);

        int changeType = getChangeType(indexVO);

        if(changeType == 0){
            return Result.buildSuccess("索引结构无任何改变");

        } else if(changeType <20){

            // 创建索引
            String indexName = indexCreator.createNewIndex(indexVO.getServiceTag(),
                     indexVO.getShardingNum(), indexVO.getReplicationNum(), indexVO.getColumns());
            // 给索引创建别名
            indexCreator.createAliasForIndex(serviceTag, indexName, indexAlias);
            // step7: reload properties
            stringRedisTemplate.convertAndSend(RELOAD_MAPPING_PROPERTIES, serviceTag);

        } else if(changeType < 30){
            // 无需重建索引，
            if(changeType == 20){
                // 只修改replica个数
                indexCreator.updateReplica(serviceTag, indexVO.getReplicationNum());
            } else {
                // 只修改mapping TODO 如果只修改了mapping？
                indexCreator.updateReplica(serviceTag, indexVO.getReplicationNum());
                indexCreator.updateMapping(serviceTag, indexVO.getColumns());
            }
        } else {

            // step1: 创建索引
            String indexName = indexCreator.createNewIndex(indexVO.getServiceTag(),
                    indexVO.getShardingNum(), indexVO.getReplicationNum(), indexVO.getColumns());

            // step2:停止copy Msg消费，并开始copy msg
            stringRedisTemplate.convertAndSend(COPY_MSG_SERVICE_TAG_STOP, serviceTag);
            stringRedisTemplate.convertAndSend(COPY_MSG_SERVICE_TAG_ADD, serviceTag);

            // step3: reIndex，为了加快reindex的速度，先设置index.refresh_interval为-1
            indexCreator.updateRefreshInterval(serviceTag, indexName, "-1");
            String taskId = reIndex(serviceTag, indexAlias, indexName, t-> {
                try {
                    //恢复index.refresh_interval值为1
                    indexCreator.updateRefreshInterval(serviceTag, indexName, "1s");
                    // step4: change alias
                    indexCreator.createAliasForIndex(serviceTag, indexName, indexAlias);

                    // step5: pause copy msg
                    stringRedisTemplate.convertAndSend(COPY_MSG_SERVICE_TAG_REMOVE, serviceTag);

                    // step6: start consume copy msg
                    stringRedisTemplate.convertAndSend(COPY_MSG_SERVICE_TAG_START, serviceTag);

                    // step7: reload properties
                    stringRedisTemplate.convertAndSend(RELOAD_MAPPING_PROPERTIES, serviceTag);

                    // step8: delete the oldest index
                    List<String> indexNameList = serviceTagIndexDao.deleteOldOne(serviceTag);
                    for(String delIndexName : indexNameList){
                        // 删除ES索引
                        indexCreator.indexDelete(serviceTag, delIndexName);
                    }
                } catch (IOException e){
                    log.error("程序异常："+e);
                }
            });
            // 保存
            saveIndexSettings(indexVO);
            return Result.buildSuccess(Maps.immutableEntry("taskId", taskId));
        }

        // 保存
        saveIndexSettings(indexVO);
        return Result.buildSuccess("index创建成功。");
    }

    private String reIndex(String serviceTag, String source, String target, Consumer consumer) throws IOException{
        Request request = new Request("POST", "/_reindex");
        request.setJsonEntity("{\n" +
                "  \"conflicts\": \"proceed\"," +
                "  \"source\": {\n" +
                "    \"index\": \""+source+"\"\n" +
                "  },\n" +
                "  \"dest\": {\n" +
                "    \"index\": \""+target+"\"\n" +
                "  }\n" +
                "}");
        request.addParameter("wait_for_completion", "false");

        Response rs = esClientFactory.getRestClient(serviceTag).performRequest(request);
        JSONObject rsJson = JSON.parseObject(EntityUtils.toString(rs.getEntity()));
        String taskId = rsJson.getString("task");

        Thread t = new Thread(() -> {
            try{
                boolean completed = false;
                do {
                    // sleep 1s
                    Thread.sleep(1000);
                    log.info("reindex 进行中，taskId:{}", taskId);
                    Request request2 = new Request("GET", "/_tasks/" + taskId);
                    Response rs2 = esClientFactory.getRestClient(serviceTag).performRequest(request2);
                    JSONObject rsJson2 = JSON.parseObject(EntityUtils.toString(rs2.getEntity()));
                    completed = rsJson2.getBoolean("completed");
                } while (!completed);
                log.info("reindex 完成，taskId:{}", taskId);
                consumer.accept(1);
                log.info("索引切换完成，taskId:{}", taskId);
            } catch (Exception e){
                log.error("程序异常", e);
            }
        });
        t.setName("reindex-task-listener");
        t.start();
        return taskId;
    }

    /**
     * 创建index
     * */
    @ApiOperation(value = "查看reindex任务进度", notes = "查看reindex任务进度",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/getReindexTaskStatus")
    public Result getReindexTaskStatus(@RequestParam String serviceTag, @RequestParam String taskId) throws IOException {
        Request request = new Request("GET", "/_tasks/" + taskId);
        Response response = esClientFactory.getRestClient(serviceTag).performRequest(request);
        JSONObject rsJson = JSON.parseObject(EntityUtils.toString(response.getEntity()));
        boolean completed = rsJson.getBoolean("completed");
        Integer total = rsJson.getJSONObject("task").getJSONObject("status").getInteger("total");
        Integer created = rsJson.getJSONObject("task").getJSONObject("status").getInteger("created");
        Integer updated = rsJson.getJSONObject("task").getJSONObject("status").getInteger("updated");
        Map rsMap = new HashMap();
        rsMap.put("completed", completed);
        rsMap.put("total", total);
        rsMap.put("updated", updated);
        rsMap.put("created", created);
        return Result.buildSuccess(rsMap);
    }

    /**
     * 分三类：0 - 无变化  1X- 为创建索引   2X- 为编辑索引    3X - 重建索引
     * changeType : 0-no change，
     * 10- create，
     * 20-setting replica change， 21-add column
     * 30- minus column, 31-data type edit,
     * 33-setting analysis edit  34-setting shard change
     * 35-analyzer changed  36- searchAble changed
     * */
    private int getChangeType(IndexVO indexVO) {

        HashSet<Integer> changeTypeSet = new HashSet<>();
        changeTypeSet.add(0);// 默认无变化

        // 判断change type
        if(indexDao.selectCount(indexVO.getServiceTag()) == 0){
            // create index
            changeTypeSet.add(10);
        } else {
            Index old = indexDao.selectOne(indexVO.getServiceTag());
            if(indexVO.getShardingNum() != old.getShardingNum()){
                // setting shard change
                changeTypeSet.add(34);
            }
            if(indexVO.getReplicationNum() != old.getReplicationNum()){
                // setting replica change
                changeTypeSet.add(20);
            }
            List<Column> oldColumn = JSON.parseArray(old.getMapping(), Column.class);
            compareMappings(changeTypeSet, indexVO.getColumns(), oldColumn);
        }

        // 返回changeType的最大值
        return changeTypeSet.stream().max(Comparator.naturalOrder()).get();
    }

    private void saveIndexSettings(IndexVO indexVO) {
        // index数据保存在DB
        Index index = new Index();
        index.setServiceTag(indexVO.getServiceTag());
        index.setShardingNum(indexVO.getShardingNum());
        index.setReplicationNum(indexVO.getReplicationNum());
        index.setMapping(JSON.toJSONString(indexVO.getColumns()));
        indexDao.insertOrUpdate(index);
    }

    /**
     * 分三类：0 - 无变化  1X- 为创建索引   2X- 为编辑索引    3X - 重建索引
     * */
    private HashSet<Integer> compareMappings(HashSet<Integer> changeTypeSet, List<Column> newMapping, List<Column> oldMapping){

        HashSet<Long> oldAnalyzerSet = new HashSet<>();
        HashSet<Long> newAnalyzerSet = new HashSet<>();

        for(int i=0; i < newMapping.size(); i++){
            boolean add = true;
            for(int j=0; j < oldMapping.size(); j++){
                if(StringUtils.equals(newMapping.get(i).getColumnName(), oldMapping.get(j).getColumnName())){
                    add = false;
                    if(newMapping.get(i).getDataType() != oldMapping.get(j).getDataType()){
                        // 数据类型修改
                        changeTypeSet.add(31);
                    }
                    if(newMapping.get(i).getSearchAble() != oldMapping.get(j).getSearchAble()){
                        // 是否可查询修改
                        changeTypeSet.add(36);
                    }
                    if(newMapping.get(i).getDataType() == DataType.TEXT_IK_SEARCH){
                        // 收集旧的mapping使用的analyzer
                        oldAnalyzerSet.add(oldMapping.get(j).getAnalyzerId());
                        if(newMapping.get(i).getAnalyzerId() != oldMapping.get(j).getAnalyzerId()){
                            // analyzer修改
                            changeTypeSet.add(35);

                        }

                    } else if(newMapping.get(i).getDataType() == DataType.NESTED ||
                            newMapping.get(i).getDataType() == DataType.OBJECT){

                        compareMappings(changeTypeSet, newMapping.get(i).getColumns(), oldMapping.get(j).getColumns());
                    }
                }
            }
            if(add) {
                changeTypeSet.add(21);
                if(newMapping.get(i).getDataType() == DataType.TEXT_IK_SEARCH) {
                    // 收集新增的analyzerId
                    newAnalyzerSet.add(newMapping.get(i).getAnalyzerId());
                }
            }
        }

        // 检查有无新增analyzer
        if(!newAnalyzerSet.stream().allMatch(r -> oldAnalyzerSet.contains(r))) {
            changeTypeSet.add(33);
        }

        // 检查有无字段删减
        for(int i=0; i < oldMapping.size(); i++){
            boolean minus = true;
            for(int j=0; j < newMapping.size(); j++){
                if(StringUtils.equals(oldMapping.get(i).getColumnName(), newMapping.get(j).getColumnName())){
                    minus = false;
                }
            }
            if(minus) changeTypeSet.add(30);
        }
        return changeTypeSet;
    }

    @ApiOperation(value = "查询支持的数据类型", notes = "查询支持的数据类型",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @GetMapping("/getDataType")
    public Result getDataType()  {
        return Result.buildSuccess(EnumSet.allOf(DataType.class));
    }
}
