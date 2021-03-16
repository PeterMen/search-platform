package com.peter.search.service.querybuilder;

import com.alibaba.fastjson.JSONArray;
import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 王海涛
 * @Description QueryBuilderIds
 * @date Created on 2018/10/16
 */
@Service("ID")
public class QueryBuilderIds extends BaseQueryBuilder implements QueryBuilder {

    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String requestName, String requestValue) {
        if(StringUtils.isEmpty(requestValue) || esQuery == null){
            return;
        }
        JSONArray jsonArray=JSONArray.parseArray(requestValue);
        List<String> list =  jsonArray.toJavaList(String.class);
        if(list == null || list.size() == 0){
            return;
        }
        Integer size = list.size();
        String[] ids = list.toArray(new String[size]);
        esQuery.setSize(size);
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder();
        idsQueryBuilder.addIds(ids);
        esQuery.setIdsQueryBuilder(idsQueryBuilder);
    }
}
