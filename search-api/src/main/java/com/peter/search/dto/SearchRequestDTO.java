package com.peter.search.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 查询请求参数名定义
 * T的类型为JSONObject或JSONArray
 * @author 王海涛
 * */
@Data
public class SearchRequestDTO<T> implements Serializable{

    /**
     * 查询参数
     * */
    private T queryParam;
    /**
     * 查询标识
     * */
    private String serviceTag;
    /**
     * 是否使用缓存
     * */
    private Boolean useCache;

    private Map<String, String> searchConfig;

    public Boolean getUseCache() {
        if(useCache == null){
            // 默认所有请求使用缓存
            this.useCache = true;
        }
        return useCache;
    }

    public void setUseCache(Boolean useCache) {
        this.useCache = useCache;
    }

    public SearchRequestDTO(){}

    /**
     * 构造查询对象
     * @param serviceTag 查询标识
     * @param queryJson 查询条件
     * */
    public SearchRequestDTO(String serviceTag, T queryJson){
        this.queryParam = queryJson;
        this.serviceTag = serviceTag;
        this.useCache = true;
    }

    @Override
    public String toString() {
        return "SearchRequestDTO{" +
                "queryParam='" + queryParam + '\'' +
                ", serviceTag='" + serviceTag + '\'' +
                ", useCache='" + useCache + '\'' +
                '}';
    }
}
