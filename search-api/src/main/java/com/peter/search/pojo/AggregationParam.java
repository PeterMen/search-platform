package com.peter.search.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 聚合查询参数
 *
 * @author 王海涛
 * */
@Getter
@Setter
@ToString
public class AggregationParam {

    /** 聚合字段 */
    private String field;
    /** 子聚合函数 */
    private String function;
    /** 自聚合字段 */
    private String functionField;
    /** 想要返回的字段 */
    private String source;
    /** 想要返回多少条 */
    private Integer fetchSize;
    /** 排序 */
    private String order;
    /** nested聚合结果的过滤条件 */
    private String filterJsonStr;

    public enum FUNCTION_ENUM {
        /**子聚合函数*/min,
        /**子聚合函数*/max,
        /**子聚合函数*/avg,
        /**子聚合函数*/sum }

    public enum ORDER {
        /**降序*/desc,
        /**升序*/asc}

    public Integer getFetchSize() {
        if(this.fetchSize == null){
            // 默认返回200条
            return 200;
        }
        return fetchSize;
    }

    @JSONField(deserialize = false)
    public void setFunction(FUNCTION_ENUM function) {
        this.function = function.name();
    }
    @JSONField(deserialize = false)
    public void setOrder(ORDER order) {
        this.order = order.name();
    }

    public void setFunction(String function) {
        this.function = function;
    }
    public void setOrder(String order) {
        this.order = order;
    }
}
