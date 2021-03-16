package com.peter.search.service.querybuilder;


import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * 设置查询路由
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "ROUTING")
public class QueryBuilderRouting extends BaseQueryBuilder implements QueryBuilder {
    

    /**
     * 设置查询路由
     * 
     * @param esQuery es query
     * @param serviceTag 业务标示
     * @param requestName 请求参数名称
     * @param requestValue 请求参数值,空，则采用默认值
     * 
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String requestName, String requestValue){
        
        if(!ObjectUtils.isEmpty(requestValue)){
           esQuery.setRouting(requestValue);
        }
    }
}
