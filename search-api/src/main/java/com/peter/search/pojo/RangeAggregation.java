package com.peter.search.pojo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 聚合查询参数
 *
 * @author 王海涛
 * */
@Getter
@Setter
@ToString
public class RangeAggregation extends AggregationParam{
    /** range类型 */
    private RangeType rangeType;
    /**  范围字段 */
    private List<Range> ranges;
    private String format;

    public enum RangeType {
        date_range,ip_range,general;
    }

    @Getter
    @Setter
    public static class Range {
        private String key;
        private String to;
        private String from;
    }
}
