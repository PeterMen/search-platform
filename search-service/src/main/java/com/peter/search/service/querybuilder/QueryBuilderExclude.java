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
@Service(value = "EXCLUDE")
public class QueryBuilderExclude extends BaseQueryBuilder implements QueryBuilder {

    /**
     * 默认要排除的字段
     * */
    private static final String DF_EXCLUDE_FIELD = "_DF_EXCLUDE_FIELD";
    
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
            String[] excludeFieldArray = paramValue.replaceAll(" ", "").split(FIELD_SPLIT_STR);

            StringBuilder fieldStr = new StringBuilder();

            // 转换为ES内部使用的字段名称
            for(String excludeField : excludeFieldArray){

                String esName = getESName(serviceTag, excludeField);
                fieldStr.append(esName).append(FIELD_SPLIT_STR);
            }
            
            fieldStr.substring(fieldStr.lastIndexOf(FIELD_SPLIT_STR), fieldStr.length());
            esQuery.setExcludes(  fieldStr.toString().split(FIELD_SPLIT_STR));

        } else {

            String dfFlField = properties.getProperty(serviceTag + DF_EXCLUDE_FIELD);
            
            if(!ObjectUtils.isEmpty(dfFlField)){
                
                // 设置默认返回字段
                esQuery.setExcludes(  dfFlField.split(FIELD_SPLIT_STR)  );
            }
        }
    }
}
