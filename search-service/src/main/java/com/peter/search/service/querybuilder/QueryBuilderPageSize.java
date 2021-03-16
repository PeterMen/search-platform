package com.peter.search.service.querybuilder;


import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * page构造器 
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "PAGE_SIZE")
public class QueryBuilderPageSize extends BaseQueryBuilder implements QueryBuilder {
    
    /**
     * 每页大小
     * */
    private static final String DF_PAGE_SIZE = "_DF_PAGE_SIZE";
    
    /**
     * 设置默认返回条数
     * 
     * @param esQuery es query
     * @param serviceTag 业务标示
     * @param requestName 请求参数名称
     * @param requestValue 请求参数值,空，则采用默认值
     * 
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String requestName, String requestValue){
        
        if(ObjectUtils.isEmpty(requestValue)){
            
            // 采用默认配置
            String pageSize = properties.getProperty(serviceTag + DF_PAGE_SIZE);
            if(!ObjectUtils.isEmpty(pageSize)){
                esQuery.setSize(Integer.parseInt(pageSize));
            }
        } else {

            int iSize = Integer.parseInt(requestValue);
            iSize = iSize > 2000 ? 2000 : iSize;
            esQuery.setSize(iSize);
        }
    }
}
