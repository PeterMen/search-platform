package com.peter.search.service.querybuilder;


import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * sort构造器
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "SORT")
public class QueryBuilderSort extends BaseQueryBuilder implements QueryBuilder {

  private static final String DEFAULT_SORT = "_DF_SORT";

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
          
          // 采用默认排序
          String defaultSort = properties.getProperty(serviceTag + DEFAULT_SORT);
          
          // 添加默认排序
          if(!ObjectUtils.isEmpty(defaultSort)){
              addSort(esQuery, serviceTag, defaultSort, true);
          }
      } else {
          
          addSort(esQuery, serviceTag, requestValue, false);
      }
  }

  /**
   * 在添加默认排序, 配置文件配置的默认排序字段一律使用es的字段名称
   * 
   * @param query 配置文件配置的排序，有一定规则
   * @param serviceTag 业务标识
   * @param sortStr 排序字段
   * @param dfSort true:默认排序 ，false:指定排序
   */
  private void addSort(QueryParam query, String serviceTag, String sortStr, Boolean dfSort) {

      String[] sortStrArray = sortStr.split(FIELD_SPLIT_STR);
      for (String str : sortStrArray) {

          // 获取参数对应的ES使用的字段名称
          String[] strArray = str.trim().split(" ");
          String esSortFieldName = getESName(serviceTag, strArray[0]);
          
          // 非默认排序，且传入的映射参数未匹配到对应的solr名称，则跳过
          if(!dfSort && ObjectUtils.isEmpty(esSortFieldName)){ continue; }
          
          if (StringUtils.equals(SortOrder.DESC.name(), strArray[1].trim().toUpperCase())) {

              // 降序
              if(dfSort){
                  query.getSortBuilderList().add(new FieldSortBuilder(strArray[0]).order(SortOrder.DESC));
              } else {
                  query.getSortBuilderList().add(new FieldSortBuilder(esSortFieldName).order(SortOrder.DESC));
              }
          } else {

              // 升序
              if(dfSort){
                  query.getSortBuilderList().add(new FieldSortBuilder(strArray[0]).order(SortOrder.ASC));
              } else {
                  query.getSortBuilderList().add(new FieldSortBuilder(esSortFieldName).order(SortOrder.ASC));
              }
          }
      }
  }
}
