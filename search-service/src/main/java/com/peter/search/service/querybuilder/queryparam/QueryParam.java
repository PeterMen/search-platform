package com.peter.search.service.querybuilder.queryparam;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.RescorerBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * query构造器基类
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
public class QueryParam {

    /**
     * 起始行
     * */
    int from;

    /**
     * 返回条数
     * */
    int size = 10;

    /**
     * 查询路由
     * */
    String routing;

    IdsQueryBuilder idsQueryBuilder ;

    /**
     * 布尔查询条件
     * */
    BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

    /**
     * 返回字段信息
     * */
    String[] includes;

    /**
     * 返回字段信息
     * */
    String[] excludes;

    /**
     * 聚合查询信息
     * */
    List<AbstractAggregationBuilder> aggregationBuilders = new ArrayList<>();

    /**
     * 高亮信息
     * */
    HighlightBuilder highlightBuilder;

    /**
     * 排序信息
     * */
    List<SortBuilder> sortBuilderList = new ArrayList<>();

    /**
     * 解析过程中使用过的解析器
     * */
    List<String> hasUsedQueryBuilderList = new ArrayList<>();

    RescorerBuilder rescoreBuilder;

    public void setRescoreBuilder(RescorerBuilder rescoreBuilder) {
        this.rescoreBuilder = rescoreBuilder;
    }

    public BoolQueryBuilder getBoolQueryBuilder() {
        return boolQueryBuilder;
    }

    public void setBoolQueryBuilder(BoolQueryBuilder boolQueryBuilder) {
        this.boolQueryBuilder = boolQueryBuilder;
    }

    public List<AbstractAggregationBuilder> getAggregationBuilders() {
        return aggregationBuilders;
    }

    public void addAggregationBuilders(AbstractAggregationBuilder aggregationBuilders) {
        this.aggregationBuilders.add(aggregationBuilders);
    }

    public String[] getIncludes() {
        return includes;
    }

    public void setIncludes(String[] includes) {
        this.includes = includes;
    }

    public String[] getExcludes() {
        return excludes;
    }

    public void setExcludes(String[] excludes) {
        this.excludes = excludes;
    }

    public HighlightBuilder getHighlightBuilder() {
        return highlightBuilder;
    }

    public void setHighlightBuilder(HighlightBuilder highlightBuilder) {
        this.highlightBuilder = highlightBuilder;
    }


    public List<SortBuilder> getSortBuilderList() {
        return sortBuilderList;
    }

    public void setSortBuilderList(List<SortBuilder> sortBuilderList) {
        this.sortBuilderList = sortBuilderList;
    }

    public List<String> getHasUsedQueryBuilderList() {
        return hasUsedQueryBuilderList;
    }

    public void setHasUsedQueryBuilderList(List<String> hasUsedQueryBuilderList) {
        this.hasUsedQueryBuilderList = hasUsedQueryBuilderList;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    public IdsQueryBuilder getIdsQueryBuilder() {
        return idsQueryBuilder;
    }

    public void setIdsQueryBuilder(IdsQueryBuilder idsQueryBuilder) {
        this.idsQueryBuilder = idsQueryBuilder;
    }

    public String getRouting() {
        return routing;
    }

    public void setRouting(String routing) {
        this.routing = routing;
    }

    /**
     * 构造SearchSourceBuilder对象
     *
     * */
    public SearchSourceBuilder getSearchSourceBuilder(){

        SearchSourceBuilder ssb = new SearchSourceBuilder();
        ssb.fetchSource(new FetchSourceContext(true, includes, excludes));
        if(!CollectionUtils.isEmpty(aggregationBuilders)){
            aggregationBuilders.forEach(aggregation -> ssb.aggregation(aggregation));
        }
        if(boolQueryBuilder != null){
            ssb.query(boolQueryBuilder);

        }
        if(idsQueryBuilder !=null){
            ssb.query(idsQueryBuilder);
        }
        if(highlightBuilder != null){
            ssb.highlighter(highlightBuilder);
        }
        ssb.from(from);
        ssb.size(size);

        for (SortBuilder sortBuilder : sortBuilderList){
            ssb.sort(sortBuilder);
        }
        if(rescoreBuilder != null){
            ssb.addRescorer(rescoreBuilder);
        }
        return ssb;
    }
}
