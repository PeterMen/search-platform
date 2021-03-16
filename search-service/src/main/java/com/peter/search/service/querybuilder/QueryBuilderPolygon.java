package com.peter.search.service.querybuilder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.GeoPolygonQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 图形搜索构造器 
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "POLYGON")
public class QueryBuilderPolygon extends BaseQueryBuilder implements QueryBuilder {

    private static final String POINT_DELIMITER = "\\|";

    /**
     * 图形搜索构造器 
     * eg:query.addFilterQuery("{!field f=xf_baidu_location}Intersects(POLYGON((-10 30, -40 40, -10 -20, 40 20, 0 0, -10 30)))");
     *
     * @param esQuery es query
     * @param serviceTag 业务标示
     * @param requestName 请求参数名称
     * @param requestValue 请求参数值
     *
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String requestName, String requestValue) {

        // 获取ES对应的字段名
        String esName = getESName(serviceTag, requestName);

        JSONArray pointArray = JSON.parseArray(requestValue);
        List<GeoPoint> points = new ArrayList<>();
        for(int i = 0; i < pointArray.size(); i++){
            String[] point = pointArray.getString(i).trim().split(POINT_DELIMITER);
            points.add(new GeoPoint(Double.valueOf(point[0]), Double.valueOf(point[1])));
        }
        GeoPolygonQueryBuilder geoPolygonFilterBuilder = new GeoPolygonQueryBuilder(esName, points);
        esQuery.getBoolQueryBuilder().filter(geoPolygonFilterBuilder);
    }
}
