package com.peter.search.service.querybuilder;

import com.peter.search.service.querybuilder.queryparam.QueryParam;
import com.peter.search.util.WebAppContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.springframework.stereotype.Service;

/**
 * @author Created by zhangfengping
 * @Description QueryBuilderWildcardAndQ
 * @date Created on 2018/11/2
 */
@Service("WILD_Q")
public class QueryBuilderWildcardAndQ extends BaseQueryBuilder implements QueryBuilder {
    /**
     * 分词器后缀
     * */
    private static final String WILD = "_WILD";
    /**
     * Q构造器
     *
     * @param esQuery solr query
     * @param serviceTag 业务标示
     * @param paramName 请求参数名称
     * @param paramValue 请求参数值,空，则采用默认值
     *
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String paramName, String paramValue) {

        if(StringUtils.isEmpty(paramValue)) { return; }

        // 设置搜索域 进行分词查询
        qParse(esQuery, serviceTag, paramName, paramValue);

        fqParse(esQuery, serviceTag, paramName, paramValue);

        esQuery.getBoolQueryBuilder().minimumShouldMatch(1);
    }

    private void fqParse(QueryParam esQuery, String serviceTag, String paramName, String paramValue) {

        String wildField = getESName(serviceTag, paramName+WILD);
        // 多字段搜索域
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for(String s : wildField.split(FIELD_SPLIT_STR)){

            //  使用工厂构造 term分词
            QueryParam subEsQuery = new QueryParam();
            QueryBuilder baseQueryBuilder = (QueryBuilder) WebAppContextUtil.getBean("FQ");
            baseQueryBuilder.buildQuery(subEsQuery, serviceTag, s, paramValue);

            subEsQuery.getBoolQueryBuilder().filter().forEach(boolQueryBuilder::should);

        }
        esQuery.getBoolQueryBuilder().should(boolQueryBuilder);
    }

    private void qParse(QueryParam esQuery, String serviceTag, String paramName, String paramValue) {
        //用Q查询器进行查询
        if(StringUtils.isEmpty(paramName) || StringUtils.isEmpty(paramValue)){ } else {

            //  使用工厂构造 term分词
            QueryParam subEsQuery = new QueryParam();
            QueryBuilder baseQueryBuilder = (QueryBuilder) WebAppContextUtil.getBean("Q");
            baseQueryBuilder.buildQuery(subEsQuery, serviceTag, paramName, paramValue);
            esQuery.getBoolQueryBuilder().should(subEsQuery.getBoolQueryBuilder());
        }
    }
}
