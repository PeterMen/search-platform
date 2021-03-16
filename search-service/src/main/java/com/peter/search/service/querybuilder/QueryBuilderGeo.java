package com.peter.search.service.querybuilder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;


/**
 * 地理位置搜索构造器
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "GEO")
public class QueryBuilderGeo extends BaseQueryBuilder implements QueryBuilder {
  
  private static final String F_LAT = "lat";
  private static final String F_LNG = "lng";
  private static final String I_DISTANCE = "distanceMeters";
  private static final String I_GEO_SORT = "iGeoSort";
  
  /**
   * query构造器
   * 
   * @param esQuery solr query
   * @param serviceTag 业务标示
   * @param paramName 请求参数名称
   * @param paramValue 请求参数值
   * */
  @Override
  public void buildQuery(QueryParam esQuery, String serviceTag, String paramName, String paramValue){

      // 获取参数对应的es名称
      String geoFieldName = getESName(serviceTag, paramName);
      JSONObject geoParam = JSON.parseObject(paramValue);

      if(geoParam.containsKey(F_LAT) && geoParam.containsKey(F_LNG)){}
      else {return;}

      //经纬度，fLat:纬度，fLng:经度, distance:半径
      if(geoParam.containsKey(I_DISTANCE)){
          GeoDistanceQueryBuilder geoQuery = QueryBuilders.geoDistanceQuery(geoFieldName)
                  .point(geoParam.getDouble(F_LAT), geoParam.getDouble(F_LNG))
                  .distance(geoParam.getString(I_DISTANCE), DistanceUnit.METERS);
          esQuery.getBoolQueryBuilder().filter(geoQuery);
      }

      // 设置排序
      if(geoParam.containsKey(I_GEO_SORT)){
          GeoDistanceSortBuilder sortBuilder = SortBuilders.geoDistanceSort(geoFieldName,
                  geoParam.getDouble(F_LAT), geoParam.getDouble(F_LNG))
                  .unit(DistanceUnit.METERS)
                  .order(StringUtils.equals(geoParam.getString(I_GEO_SORT).toUpperCase(), SortOrder.DESC.name()) ? SortOrder.DESC : SortOrder.ASC );
          esQuery.getSortBuilderList().add(sortBuilder);
      }
  }
}
