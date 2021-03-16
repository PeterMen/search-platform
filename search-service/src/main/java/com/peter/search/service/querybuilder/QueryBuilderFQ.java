package com.peter.search.service.querybuilder;

import com.peter.search.service.querybuilder.queryparam.QueryParam;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.*;
import org.springframework.stereotype.Service;

/**
 * FQ构造器基类
 *
 * @author 七星
 * @date 2018年02月02日
 * @version 1.0
 */
@Service(value = "FQ")
public class QueryBuilderFQ extends BaseQueryBuilder implements QueryBuilder {

    public static final String EXCLAMATION = "!";
    public static final String COLON_BEHALF = "&colon;";
    public static final String COLON = ":";

    /**
     * FQ类型枚举
     * */
    public enum FQ_TYPE {
        /**大于*/GT("gt"),
        /**小于*/LT("lt"),
        /**大于等于*/GTE("gte"),
        /**小于等于*/LTE("lte"),
        /**或*/OR("or"),
        /**且*/AND("and"),
        /**区间*/RA("ra"),
        /**左闭右开*/RAL("ral"),
        /**左开右闭*/RAR("rar"),
        /**前缀匹配*/PREFIX("prefix"),
        /**模糊查询*/WILDCARD("wildcard");

        private String name;

        FQ_TYPE(String name){
            this.name = name;
        }
        /**
         * get set 方法
         * */
        public String getName() {
            return name;
        }
    }
  
    /**
     * query构造器
     * 
     * @param esQuery solr query
     * @param serviceTag 业务标示
     * @param paramName 请求参数名称
     * @param paramValue 请求参数值
     * 
     * 规则名 传参格式                   解析后的格式             备注
     * gt   gt:5    {5 TO *}    大于
     * lt   lt:5    {* TO 5}    小于
     * ge   ge:5    [5 TO *]    大于等于
     * le   le:5    [* TO 5]    小于等于
     * or   or:1,10,15  (1 OR10 OR 15)  or
     * an   an:1,10,15  (1 AND 10 AND 15)   and
     * ra   ra:1,100 [1 TO 100]  闭合区间
     * ral  ral:1,100 [1 TO 100 } 左闭右开区间
     * rar  rar:1,100 {1 TO 100 ] 左开右闭区间
     * not  !xxx:23
     * 
     * */
    @Override
    public void buildQuery(QueryParam esQuery, String serviceTag, String paramName, String paramValue){

        BoolQueryBuilder boolQueryBuilder = esQuery.getBoolQueryBuilder();

        // 取es映射名称
        String esName = getESName(serviceTag, paramName);
        
        // 判斷是否取反
        boolean mustNot = false;
        if(paramValue.startsWith(EXCLAMATION)){
            mustNot = true;
            // 去除感叹号
            paramValue = paramValue.substring(1);
            if(paramValue.startsWith(COLON)){
                // 如果去掉！之后，是冒号开始的，说明没有规则符，需要把冒号删除
                paramValue = paramValue.substring(1);

            }
        }

        // 判断是否nested结构查询
        boolean nested = isNested(esName, serviceTag);

        int charAt = paramValue.indexOf(COLON);
        if(charAt == -1){
            // 日期格式中，冒号需要转义
            buildNoRule(boolQueryBuilder, esName, mustNot, nested, paramValue.replaceAll(COLON_BEHALF, COLON));
        } else {

            // 规则名，规则值
            String ruleName = paramValue.substring(0, charAt);
            String ruleVal = paramValue.substring(charAt+1).replaceAll(COLON_BEHALF, COLON);

            // 枚举名称转为枚举类型
            FQ_TYPE fqType = getEnum(ruleName.toUpperCase());

            if(FQ_TYPE.GT == fqType){
                buildGt(boolQueryBuilder, esName, mustNot, nested, ruleVal);
            } else if(FQ_TYPE.LT== fqType){
                buildLt(boolQueryBuilder, esName, mustNot, nested, ruleVal);
            } else if(FQ_TYPE.GTE== fqType){
                buildGte(boolQueryBuilder, esName, mustNot, nested, ruleVal);
            } else if(FQ_TYPE.LTE == fqType){
                buildLte(boolQueryBuilder, esName, mustNot, nested, ruleVal);
            } else if(FQ_TYPE.OR == fqType){
                buildOr(boolQueryBuilder, esName, mustNot, nested, ruleVal);
            } else if(FQ_TYPE.AND == fqType){
                buildAnd(boolQueryBuilder, esName, mustNot, nested, ruleVal);
            } else if(FQ_TYPE.RA == fqType){
                buildRa(boolQueryBuilder, esName, mustNot, nested, ruleVal, true, true);
            } else if(FQ_TYPE.RAL == fqType){
                buildRa(boolQueryBuilder, esName, mustNot, nested, ruleVal, true, false);
            } else if(FQ_TYPE.RAR == fqType){
                buildRa(boolQueryBuilder, esName, mustNot, nested, ruleVal, false, true);
            } else if(FQ_TYPE.PREFIX == fqType){
                buildPrefix(boolQueryBuilder, esName, mustNot, nested, ruleVal);
            } else if(FQ_TYPE.WILDCARD == fqType){
                buildWildcard(boolQueryBuilder, esName, mustNot, nested, ruleVal);
            } else {
                // 规则名不存在，按无规则方式处理
                buildNoRule(boolQueryBuilder, esName, mustNot, nested, paramValue.replaceAll(COLON_BEHALF, COLON));
            }
        }
    }

    /**
     * 枚举名称转为枚举类型
     * */
    private FQ_TYPE getEnum(String enumName){
        try{
            return FQ_TYPE.valueOf(enumName);
        } catch (EnumConstantNotPresentException ex){
            return null;
        } catch (IllegalArgumentException ex){
            return null;
        }
    }

    /**
     * 无规则FQ
     * */
    private void buildNoRule(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {

        // 等于
        if(mustNot){
            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), QueryBuilders.termQuery(esName, ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(new TermQueryBuilder(esName, ruleVal));
            }

        } else {
            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), QueryBuilders.termQuery(esName, ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(new TermQueryBuilder(esName, ruleVal));
            }
        }
    }

    private void buildRa(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal, boolean left, boolean right) {
        String[] arr = ruleVal.split(FIELD_SPLIT_STR);
        if (arr.length > 1) {

            RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(esName)
                    .from(arr[0])
                    .to(arr[1])
                    .includeLower(left)
                    .includeUpper(right);

            if (mustNot) {

                if (nested) {
                    boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), rangeQueryBuilder, ScoreMode.Total));
                } else {
                    boolQueryBuilder.mustNot(rangeQueryBuilder);
                }
            } else {

                if (nested) {
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), rangeQueryBuilder, ScoreMode.Total));
                } else {
                    boolQueryBuilder.filter(rangeQueryBuilder);
                }
            }
        }
    }

    private void buildAnd(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {
        BoolQueryBuilder subBoolQueryBuilder = new BoolQueryBuilder();

        String[] arr = ruleVal.split(FIELD_SPLIT_STR);
        if (arr.length > 1) {
            for (String s : arr) {
                subBoolQueryBuilder.filter(new TermQueryBuilder(esName, s));
            }
        } else {
            subBoolQueryBuilder.filter(new TermQueryBuilder(esName, arr[0]));
        }

        if(mustNot){

            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), subBoolQueryBuilder, ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(subBoolQueryBuilder);
            }
        }else {

            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), subBoolQueryBuilder, ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(subBoolQueryBuilder);
            }
        }
    }

    private void buildWildcard(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {
        WildcardQueryBuilder subBoolQueryBuilder = new WildcardQueryBuilder(esName, SNOW + ruleVal+ SNOW);
        if(mustNot){
            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), subBoolQueryBuilder, ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(subBoolQueryBuilder);
            }
        }else {
            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), subBoolQueryBuilder, ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(subBoolQueryBuilder);
            }
        }
    }

    private void buildPrefix(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {
        PrefixQueryBuilder subBoolQueryBuilder = new PrefixQueryBuilder(esName, ruleVal);
        if(mustNot){
            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), subBoolQueryBuilder, ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(subBoolQueryBuilder);
            }
        }else {
            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), subBoolQueryBuilder, ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(subBoolQueryBuilder);
            }
        }
    }

    private void buildOr(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {
        BoolQueryBuilder subBoolQueryBuilder = new BoolQueryBuilder();

        // 包含
        String[] arr = ruleVal.split(FIELD_SPLIT_STR);
        if (arr.length > 1) {
            for (String s : arr) {
                subBoolQueryBuilder.should(new TermQueryBuilder(esName, s));
            }
        } else {
            subBoolQueryBuilder.should(new TermQueryBuilder(esName, arr[0]));
        }

        if(mustNot){

            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), subBoolQueryBuilder, ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(subBoolQueryBuilder);
            }
        }else {

            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), subBoolQueryBuilder, ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(subBoolQueryBuilder);
            }
        }
    }

    private void buildLte(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {
        // 小于等于
        if(mustNot){

            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), new RangeQueryBuilder(esName).lte(ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(new RangeQueryBuilder(esName).lte(ruleVal));
            }
        }else {

            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), new RangeQueryBuilder(esName).lte(ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(new RangeQueryBuilder(esName).lte(ruleVal));
            }
        }
    }

    private void buildGte(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {
        // 大于等于
        if(mustNot){

            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), new RangeQueryBuilder(esName).gte(ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(new RangeQueryBuilder(esName).gte(ruleVal));
            }
        }else {

            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), new RangeQueryBuilder(esName).gte(ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(new RangeQueryBuilder(esName).gte(ruleVal));
            }
        }
    }

    private void buildLt(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {
        // 小于
        if(mustNot){

            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), new RangeQueryBuilder(esName).lt(ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(new RangeQueryBuilder(esName).lt(ruleVal));
            }
        }else {

            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), new RangeQueryBuilder(esName).lt(ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(new RangeQueryBuilder(esName).lt(ruleVal));
            }
        }
    }

    private void buildGt(BoolQueryBuilder boolQueryBuilder, String esName, boolean mustNot, boolean nested, String ruleVal) {
        // 大于
        if(mustNot){
            if(nested){
                boolQueryBuilder.mustNot(QueryBuilders.nestedQuery(getNestedPath(esName), new RangeQueryBuilder(esName).gt(ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.mustNot(new RangeQueryBuilder(esName).gt(ruleVal));
            }
        }else {

            if(nested){
                boolQueryBuilder.filter(QueryBuilders.nestedQuery(getNestedPath(esName), new RangeQueryBuilder(esName).gt(ruleVal), ScoreMode.Total));
            } else {
                boolQueryBuilder.filter(new RangeQueryBuilder(esName).gt(ruleVal));
            }
        }
    }

}
