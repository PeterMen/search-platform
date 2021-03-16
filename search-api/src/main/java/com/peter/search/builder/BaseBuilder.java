package com.peter.search.builder;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.List;

abstract class BaseBuilder {

    protected static final String ES_NAME = "_ES_NAME";
    protected static final String QUERY_BUILDER = "_QUERY_BUILDER";

    protected JSONObject queryParam = new JSONObject();
    protected List<ImmutablePair<String, QUERY>> queryBuilder = new ArrayList<>();
    protected List<ImmutablePair<String, String>> nameMapping = new ArrayList<>();

    protected void setNameMapping(String name, String esName){
        nameMapping.add(ImmutablePair.of(name, esName));
    }

    protected void setQueryBuilder(String name, QUERY query){
        queryBuilder.add(ImmutablePair.of(name, query));
    }

    protected void addQueryBuilder(List<ImmutablePair<String, QUERY>> t){
        queryBuilder.addAll(t);
    }

    protected void addNameMapping(List<ImmutablePair<String, String>> t){
        nameMapping.addAll(t);
    }

    /**
     * 所有可用的查询解析器名称
     * */
    public enum QUERY{ FQ, PAGE_START, PAGE_SIZE, ROUTING,
        POLYGON, SORT, SORT_MODE, FL, HL,AGGREGATION, RANGE_AGGREGATION, GEO, MULTIPLE_OR,
        Q, MULTIPLE_AND, WILDCARD, RANGE_START, RANGE_END, ID, RESCORE_ID, RESCORE}



}
