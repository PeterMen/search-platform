package com.peter.search.service.querybuilder;


import com.alibaba.fastjson.JSON;
import com.peter.search.pojo.ReScore;
import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.rescore.QueryRescoreMode;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import tk.mybatis.mapper.util.Assert;

/**
 * 二次排序构造器-包含参数
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "RESCORE")
public class QueryBuilderRescore extends BaseQueryBuilder implements QueryBuilder {

  /**
   * sort构造器
   * 
   * @param esQuery solr query
   * @param serviceTag 业务标示
   * @param requestName 请求参数名称
   * @param reScoreValue 请求参数值,空，则采用默认值
   * 
   * */
  @Override
  public void buildQuery(QueryParam esQuery, String serviceTag, String requestName, String reScoreValue) {

      // 没有传入 scriptId
      if(ObjectUtils.isEmpty(reScoreValue)){
         return;
      }
      ReScore reScore = JSON.parseObject(reScoreValue, ReScore.class);
      Assert.notEmpty(reScore.getReScoreId(), "reScoreId不能为空");

      Script script = new Script(ScriptType.STORED, null, reScore.getReScoreId(), reScore.getParams());
      esQuery.setRescoreBuilder(new QueryRescorerBuilder(
              QueryBuilders.functionScoreQuery(new ScriptScoreFunctionBuilder(script))
      ).windowSize(reScore.getWindowSize()).setScoreMode(QueryRescoreMode.Max));
  }
}
