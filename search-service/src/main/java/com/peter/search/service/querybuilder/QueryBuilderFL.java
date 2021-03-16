package com.peter.search.service.querybuilder;


import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * 返回字段构造器 
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "FL")
public class QueryBuilderFL extends BaseQueryBuilder implements QueryBuilder {

    /**
     * 默认返回字段
     * */
    private static final String DF_FL_FIELD = "_DF_FL_FIELD";
    
    /**
     * query构造器
     * 
     * @param esQuery solr query
     * @param serviceTag 业务标示
     * @param paramName 请求参数名称
     * @param paramValue 请求参数值
     * 
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String paramName, String paramValue){

        if(!ObjectUtils.isEmpty(paramValue)){
            
            // 传入指定返回字段
            String[] flFieldArray = paramValue.replaceAll(" ", "").split(FIELD_SPLIT_STR);

            StringBuilder fieldStr = new StringBuilder();

            // 转换为ES内部使用的字段名称
            for(String flField : flFieldArray){

                String esName = getESName(serviceTag, flField);
                fieldStr.append(esName).append(FIELD_SPLIT_STR);
            }
            
            fieldStr.substring(fieldStr.lastIndexOf(FIELD_SPLIT_STR), fieldStr.length());
            esQuery.setIncludes( fieldStr.toString().split(FIELD_SPLIT_STR));

        } else {

            String dfFlField = properties.getProperty(serviceTag + DF_FL_FIELD);
            
            if(!ObjectUtils.isEmpty(dfFlField)){
                
                // 设置默认返回字段
                esQuery.setIncludes(dfFlField.split(FIELD_SPLIT_STR));
            }
        }
    }
}
