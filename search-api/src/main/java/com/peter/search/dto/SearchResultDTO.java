package com.peter.search.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 返回结果集封装
 *
 * @author 王海涛
 * @version 1.0
 *
 */
public class SearchResultDTO implements Serializable {

    public SearchResultDTO(){}
    private static final long serialVersionUID = -30002886874112287L;

    /**
     * 状态码 -- 失败
     */
    public static final int STATUS_FAILED = 0;

    /**
     * 状态码 -- 成功
     */
    public static final int STATUS_SUCCESS = 1;

    public int getMaxReturnRow() {
        return maxReturnRow;
    }

    private final int maxReturnRow = 5000;

    /**
     * 1:成功;0失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errMsg;

    /**
     * 相应时间
     */
    private Long tookMillis;

    /**
     * 总条数
     */
    private Long totalNum;

    private String scrollId;

    /**
     * 实际返回条数
     */
    private Integer returnSize;

    private ArrayList<Map> data;

    private Map<String, List<AggregationData>> aggregationDataMap;

    public Map<String, List<AggregationData>> getAggregationDataMap() {
        if(this.aggregationDataMap == null){
            this.aggregationDataMap = new HashMap<>();
        }
        return aggregationDataMap;
    }

    public void addAggregationData(String aggKey, AggregationData aggData) {
        if(getAggregationDataMap().get(aggKey) == null){
            getAggregationDataMap().put(aggKey, new ArrayList<>());
        }
        getAggregationDataMap().get(aggKey).add(aggData);
    }

    public void setAggregationDataMap(Map<String, List<AggregationData>> aggregationDataMap) {
        this.aggregationDataMap = aggregationDataMap;
    }

    public List<Map> getData() {
        if(data == null){
            data = new ArrayList<>();
        }
        return data;
    }

    public void setData(ArrayList<Map> data) {
        this.data = data;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getTotalNum() {
        if(totalNum == null){
            totalNum = 0L;
        }
        return totalNum;
    }

    public void setTotalNum(Long totalNum) {
        this.totalNum = totalNum;
    }

    public SearchResultDTO(int status) {
        this.status = status;
    }

    public Integer getReturnSize() {
        return returnSize;
    }

    public void setReturnSize(Integer returnSize) {
        this.returnSize = returnSize;
    }

    public static int getStatusFailed() {
        return STATUS_FAILED;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public Long getTookMillis() {
        return tookMillis;
    }

    public void setTookMillis(Long tookMillis) {
        this.tookMillis = tookMillis;
    }

    public static class AggregationData implements Serializable{

        /**
         * 被聚合的field对应的value,比如，按cityId聚合 ，key=289
         * */
        private String key;

        private Long count;
        private Double max;
        private Double min;
        private Double sum;
        private Double avg;
        /**
         * 聚合返回数据
         * */
        private HashMap sourceData;


        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Long getCount() {
            if(count == null){
                this.count = 0L;
            }
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public Double getMax() {
            return max;
        }

        public void setMax(Double max) {
            this.max = max;
        }

        public Double getMin() {
            return min;
        }

        public void setMin(Double min) {
            this.min = min;
        }

        public Double getSum() {
            return sum;
        }

        public void setSum(Double sum) {
            this.sum = sum;
        }

        public Double getAvg() {
            return avg;
        }

        public void setAvg(Double avg) {
            this.avg = avg;
        }

        public Map getSourceData() {
            if(sourceData == null){
                this.sourceData = new HashMap(16);
            }
            return sourceData;
        }

        public void setSourceData(Map sourceData) {
            if(sourceData instanceof HashMap){
                this.sourceData = (HashMap) sourceData;
            } else {
                this.sourceData = new HashMap(sourceData);
            }
        }
    }

    public String getScrollId() {
        return scrollId;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }

    @Override
    public String toString() {
        return "SearchResultDTO{" +
                "status=" + status +
                ", errMsg='" + errMsg + '\'' +
                ", tookMillis=" + tookMillis +
                ", totalNum=" + totalNum +
                ", returnSize=" + returnSize +
                ", data=" + data +
                ", aggregationDataList=" + aggregationDataMap +
                '}';
    }
}
