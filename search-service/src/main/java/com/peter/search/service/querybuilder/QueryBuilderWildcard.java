package com.peter.search.service.querybuilder;


import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.springframework.stereotype.Service;

/**
 * 通配符构造器
 * 
 * @author 七星
 * @date 2017年03月02日
 * @version 1.0
 */
@Service(value = "WILDCARD")
public class QueryBuilderWildcard extends BaseQueryBuilder implements QueryBuilder {
    
    public static final String name = "WILDCARD";

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

        // 设置搜索域
        String searchField = getESName(serviceTag, paramName);
        if(searchField.indexOf(FIELD_SPLIT_CHAR) == -1){
            // 判断是否nested结构查询
            if(isNested(searchField, serviceTag)){
                // nested 结构字段查询
                esQuery.getBoolQueryBuilder().filter(QueryBuilders.nestedQuery(getNestedPath(searchField),
                        getCaseInSensitiveQuery(searchField, paramValue), ScoreMode.Total));
            } else {
                // 非nested结构查询
                // 单字段搜索域
                esQuery.getBoolQueryBuilder().filter(getCaseInSensitiveQuery(searchField, paramValue));
            }

        } else {
            // 多字段搜索域
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            for(String s : searchField.split(FIELD_SPLIT_STR)){
                // 判断是否nested结构查询
                if(isNested(s, serviceTag)){
                    boolQueryBuilder.should(QueryBuilders.nestedQuery(getNestedPath(s), getCaseInSensitiveQuery(s, paramValue), ScoreMode.Total));
                } else {
                    // 非nested结构字段查询
                    boolQueryBuilder.should(getCaseInSensitiveQuery(s, paramValue));
                }
            }
            esQuery.getBoolQueryBuilder().filter(boolQueryBuilder);
        }

    }
    /**
     * 不区分大小写
     * */
    private AbstractQueryBuilder getCaseInSensitiveQuery(String searchField, String searchValue){
        if(containsLetter(searchValue)){
            return QueryBuilders.boolQuery()
                    .should(new WildcardQueryBuilder(searchField, SNOW +searchValue.toLowerCase()+SNOW))
                    .should(new WildcardQueryBuilder(searchField, SNOW+searchValue.toUpperCase()+SNOW));
        } else {
            return new WildcardQueryBuilder(searchField, SNOW+searchValue+SNOW);
        }
    }

    /**
     * 是否包含纯字母
     * @param str 搜索关键字
     * @return
     */
    private boolean containsLetter(String str) {
        char[] chars = str.toCharArray();
        boolean isSmallLetter;
        boolean isBigLetter;
        for (int i = 0; i < chars.length; i++) {
            isBigLetter = (chars[i] >= 'A' && chars[i] <= 'Z');
            isSmallLetter = (chars[i] >= 'a' && chars[i] <= 'z');
            if (isSmallLetter || isBigLetter) {
                return true;
            }
        }
        return false;
    }
}
