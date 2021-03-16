package com.peter.search.builder;

import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.pojo.AggregationParam;
import com.peter.search.pojo.GeoParam;
import com.peter.search.pojo.RangeAggregation;
import com.peter.search.pojo.ReScore;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;

/**
 * 查询请求构造器
 * */
public class SearchRequestBuilder extends FQBuilder<SearchRequestBuilder>{

    private static final String OR_KEY = "or";
    private static final String AND_KEY = "and";
    private static final String RE_SCORE_KEY = "reScore";
    private static final String SORT_MODE_KEY = "sSortMode";
    private static final String GEO_KEY = "geo";
    private static final String POLYGON_KEY = "polygon";
    private static final String DOC_IDS_KEY = "docIds";
    private static final String RE_SCORE_ID_KEY = "reScoreId";
    private static final String ROUTING_KEY = "routing";
    private static final String FL_KEY = "sFLParam";
    private static final String S_KW_KEY = "sKw";
    private static final String AGGS_KEY = "aggs";
    private static final String RANG_AGGS_KEY = "rangAggs";
    private static final String SORT_KEY = "sSort";
    private static final String START_KEY = "iStart";
    private static final String ROW_SIZE_KEY = "iRowSize";
    public static final String SPLIT_CHAR = "_";
    public static final String WILDCARD_KEY = "wildcard";

    private String serviceTag;
    private Integer orSize = 0;
    private Integer andSize = 0;
    private Integer polygonSize = 0;
    private Integer wildcardSize = 0;
    private Integer geoSize = 0;
    private Integer aggSize = 0;
    private Integer rangeAggSize = 0;

    /**
     * 添加or组合条件
     * */
    public SearchRequestBuilder addOR(ORBuilder or){
        if(or.queryParam.size() != 0){
            String key = OR_KEY + orSize++;
            queryParam.put(key, or.queryParam);
            setQueryBuilder(key, QUERY.MULTIPLE_OR);
            addNameMapping(or.nameMapping);
            addQueryBuilder(or.queryBuilder);
        }
        return this;
    }

    /**
     * 添加and组合条件
     * */
    public SearchRequestBuilder addAnd(AndBuilder and){
        if(and.queryParam.size() != 0){
            String key = AND_KEY + andSize++;
            queryParam.put(key, and.queryParam);
            setQueryBuilder(key, QUERY.MULTIPLE_AND);
            addNameMapping(and.nameMapping);
            addQueryBuilder(and.queryBuilder);
        }
        return this;
    }

    /**
     * 设置图形查询条件
     * */
    public SearchRequestBuilder polygon(List<String> polygon){
        String key = POLYGON_KEY + polygonSize++;
        queryParam.put(key, polygon);
        setQueryBuilder(key, QUERY.POLYGON);
        return this;
    }

    /**
     * 设置模糊查询
     * esName支持多字段查询，逗号隔开，多字段之间是或的关系
     * */
    public SearchRequestBuilder addWildcard(String esName, String value){
        String key = WILDCARD_KEY + wildcardSize++;
        queryParam.put(key, value);
        setQueryBuilder(key, QUERY.WILDCARD);
        setNameMapping(key, esName);
        return this;
    }

    /**
     * 设置二次排序
     * */
    public SearchRequestBuilder reScore(ReScore reScore){
        queryParam.put(RE_SCORE_KEY, reScore);
        return this;
    }

    /**
     * 设置排序模式
     * */
    public SearchRequestBuilder sortMode(String sortMode){
        queryParam.put(SORT_MODE_KEY, sortMode);
        return this;
    }

    /**
     * 添加圆圈查询
     * */
    public SearchRequestBuilder addGeo(GeoParam geoParam){
        String key = GEO_KEY + geoSize++;
        queryParam.put(key, geoParam);
        setQueryBuilder(key, QUERY.GEO);
        return this;
    }

    /**
     * 查询文档ID
     * */
    public SearchRequestBuilder docIds(List<String> ids){
        queryParam.put(DOC_IDS_KEY, ids);
        return this;
    }

    /**
     * 设置业务标识
     * */
    public SearchRequestBuilder serviceTag(String serviceTag){
        this.serviceTag = serviceTag;
        return this;
    }

    /**
     * 设置二次查询ID
     * */
    public SearchRequestBuilder reScoreId(String reScoreId){
        queryParam.put(RE_SCORE_ID_KEY, reScoreId);
        return this;
    }

    /**
     * 设置查询路由
     * */
    public SearchRequestBuilder routing(String routing){
        queryParam.put(ROUTING_KEY, routing);
        return this;
    }

    /**
     * 设置返回字段
     * */
    public SearchRequestBuilder fl(String fl){
        queryParam.put(FL_KEY, fl);
        return this;
    }

    /**
     * 设置关键字查询
     * */
    public SearchRequestBuilder sKw(String fields, String value){
        queryParam.put(S_KW_KEY, value);
        setNameMapping(S_KW_KEY, fields);
        return this;
    }

    /**
     * 设置聚合查询
     * */
    public SearchRequestBuilder addAggregation(AggregationParam agg){
        String key = AGGS_KEY + aggSize++;
        queryParam.put(key, agg);
        setQueryBuilder(key, QUERY.AGGREGATION);
        return this;
    }

    /**
     * 设置范围聚合查询
     * */
    public SearchRequestBuilder addRangeAgg(RangeAggregation agg){
        String key = RANG_AGGS_KEY + rangeAggSize++;
        queryParam.put(key, agg);
        setQueryBuilder(key, QUERY.RANGE_AGGREGATION);
        return this;
    }

    /**
     * 设置排序条件，多个排序条件用逗号隔开
     * */
    public SearchRequestBuilder sort(String sort){
        queryParam.put(SORT_KEY, sort);
        return this;
    }

    /**
     * 设置分页的起始行
     * */
    public SearchRequestBuilder iStart(Integer iStart){
        queryParam.put(START_KEY, iStart);
        return this;
    }

    /**
     * 设置返回条数
     * */
    public SearchRequestBuilder iRowSize(Integer iRowSize){
        queryParam.put(ROW_SIZE_KEY, iRowSize);
        return this;
    }

    /**
     * OR 构造器
     * */
    @Getter
    public static class ORBuilder extends FQBuilder<ORBuilder>{
        private Integer andSize = 0;
        public ORBuilder addAnd(AndBuilder and){
            String key = AND_KEY + andSize++;
            queryParam.put(key, and.queryParam);
            setQueryBuilder(key, QUERY.MULTIPLE_AND);
            addNameMapping(and.nameMapping);
            addQueryBuilder(and.queryBuilder);
            return this;
        }
    }

    /**
     * and构造器
     * */
    @Getter
    public static class AndBuilder extends FQBuilder<AndBuilder>{
        private Integer orSize = 0;
        public AndBuilder addOR(ORBuilder or){
            String key = OR_KEY + orSize++;
            queryParam.put(key, or.queryParam);
            setQueryBuilder(key, QUERY.MULTIPLE_OR);
            addNameMapping(or.nameMapping);
            addQueryBuilder(or.queryBuilder);
            return this;
        }
    }

    /**
     * 构造查询请求对象
     * */
    public SearchRequestDTO<JSONObject> build(){

        SearchRequestDTO searchRequestDTO = new SearchRequestDTO();
        searchRequestDTO.setUseCache(true);
        searchRequestDTO.setServiceTag(this.serviceTag);
        searchRequestDTO.setQueryParam(queryParam);
        HashMap<String, String> searchConfig = new HashMap<>();
        nameMapping.forEach(config -> searchConfig.put(config.left+ SPLIT_CHAR +serviceTag+ES_NAME, config.right));
        queryBuilder.forEach(config -> searchConfig.put(config.left+SPLIT_CHAR+serviceTag+QUERY_BUILDER, config.right.name()));
        searchRequestDTO.setSearchConfig(searchConfig);

        return searchRequestDTO;
    }
}
