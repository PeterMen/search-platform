package com.peter.search.datametrics;


import lombok.Data;

@Data
public class SearchRequestDataMetrics {

    public static final String SLOW_QUERY_TIME = "slow.query.min.time.millis";

    private String serviceTag;
    private Long timeMS;
    private Long spendTimeMS;
    private Boolean hasException;
    private Object slowQuery;

    public SearchRequestDataMetrics(String serviceTag, Long spendTimeMS, Boolean hasException, Object query){
        this.serviceTag = serviceTag;
        this.spendTimeMS = spendTimeMS;
        this.timeMS = System.currentTimeMillis();
        this.hasException = hasException;
        this.slowQuery = query;
    }
}
