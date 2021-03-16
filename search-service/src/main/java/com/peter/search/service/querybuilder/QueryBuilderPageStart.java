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
@Service(value = "PAGE_START")
public class QueryBuilderPageStart extends BaseQueryBuilder implements QueryBuilder {
    
    /**
     * 从第几行开始
     * */
    private static final String DF_PAGE_START = "_DF_PAGE_START";
    
    /**
     * 设置起始行
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
            String pageStart = properties.getProperty(serviceTag + DF_PAGE_START);
            if(!ObjectUtils.isEmpty(pageStart)){
                esQuery.setFrom(Integer.parseInt(pageStart));
            }
        } else {

            int iStart = Integer.parseInt(requestValue);
            iStart = iStart > 5000 ? 5000: iStart;
            esQuery.setFrom(iStart);
        }
    }
}
