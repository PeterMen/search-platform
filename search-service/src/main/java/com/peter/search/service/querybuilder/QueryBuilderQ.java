package com.peter.search.service.querybuilder;

import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Q构造器基类
 * 
 * @author 七星
 * @date 2017年03月02日
 * @version 1.0
 */
@Service(value = "Q")
public class QueryBuilderQ extends BaseQueryBuilder implements QueryBuilder {

    /**
     * 权重分隔符
     * */
    public static final String S_S = "^";

    /**
     * 分词器后缀
     * */
    private static final String ANALYZER = "_Q_ANALYZER";

    /**
     * Q构造器
     * 
     * @param esQuery solr query
     * @param serviceTag 业务标示
     * @param paramName 请求参数名称
     * @param paramValue 请求参数值,空，则采用默认值
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String paramName, String paramValue){

        // 设置默认搜索域
        String sKwSearchFd = getESName(serviceTag, paramName);

        // 未配置默认搜索域
        if(StringUtils.isEmpty(sKwSearchFd) || StringUtils.isEmpty(paramValue)){ return; }

        Map<String, Float> fieldsBoosts = new HashMap<>(16);
        String[] sKwFields = sKwSearchFd.split(FIELD_SPLIT_STR);

        // 解析权重字段
        for(int i = 0; i < sKwFields.length; i++){
            if(StringUtils.isNotEmpty(sKwFields[i]) && sKwFields[i].contains(S_S)){
                int splitCharIndex = sKwFields[i].indexOf(S_S);
                String key = sKwFields[i].substring(0, splitCharIndex);
                fieldsBoosts.put(key, Float.parseFloat(sKwFields[i].substring(splitCharIndex+1)));
                sKwFields[i] = key;
            }
        }
        // 跨字段搜索，支持权重配置
        MultiMatchQueryBuilder mmm = new MultiMatchQueryBuilder(paramValue, sKwFields);

        // 设置权重
        mmm.fields(fieldsBoosts);

        /**
         * if tie_breaker is specified, then it calculates the score as follows:
         *  - the score from the best matching field
         * - plus tie_breaker * _score for all other matching fields
         **/
        mmm.tieBreaker(0.3f);

        // 设置分词器
        String analyzer = properties.getProperty(paramName+"_"+serviceTag+ANALYZER);
        mmm.analyzer(analyzer);

        esQuery.getBoolQueryBuilder().must(mmm);
    }
}