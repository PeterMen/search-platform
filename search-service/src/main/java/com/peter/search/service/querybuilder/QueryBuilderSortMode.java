package com.peter.search.service.querybuilder;


import com.peter.search.pojo.SortMode;
import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * sort构造器
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "SORT_MODE")
public class QueryBuilderSortMode extends BaseQueryBuilder implements QueryBuilder {

  /**
   * sort构造器
   * 
   * @param esQuery solr query
   * @param serviceTag 业务标示
   * @param requestName 请求参数名称
   * @param requestValue 请求参数值,空，则采用默认值
   * 
   * */
  @Override
  public void buildQuery(QueryParam esQuery, String serviceTag, String requestName, String requestValue) {

      // 没有传入排序字段
      if(ObjectUtils.isEmpty(requestValue)){
         return;
      }

      // 随机排序
      if(StringUtils.equals(requestValue, SortMode.RANDOM.name())){
          Script script = new Script("Math.random()");
          ScriptSortBuilder scriptSortBuilder = new ScriptSortBuilder(script, ScriptSortBuilder.ScriptSortType.NUMBER);
          esQuery.getSortBuilderList().add(scriptSortBuilder);
      }
  }
}
