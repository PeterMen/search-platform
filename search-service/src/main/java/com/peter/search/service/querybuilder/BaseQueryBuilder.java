package com.peter.search.service.querybuilder;

import com.peter.search.util.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 解析器基类
 *
 * @author 王海涛
 * */
public abstract class BaseQueryBuilder {

    @Autowired
    protected PropertyUtils properties;

    /**
     * key解析之后放入boolQueryBuilder的类型，默认是must,
     * must should
     * */
    public static final String MINIMUM_SHOULD_MATCH = "_MINIMUM_SHOULD_MATCH";
    /**
     * ES name
     * */
    public static final String ES_NAME = "_ES_NAME";

    /**
     * ES name
     * */
    public static final String NESTED = "_NESTED";

    /**
     * nested结构字段分隔符
     * */
    public static final char NESTED_SPLIT_CHAR = '.';

    /**
     * nested结构字段分隔符
     * */
    public static final char SPLIT_CHAR = '_';

    /**
     * 多字段值分隔符
     * */
    public static final char FIELD_SPLIT_CHAR = ',';

    /**
     * 多字段值分隔符
     * */
    public static final String FIELD_SPLIT_STR= ",";

    /**
     * json数组首字符
     * */
    public static final String JSON_ARRAY_PREFIX = "[";

    public static final String SNOW = "*";

    /**
     * 获取ES内部使用的字段名称
     * @param serviceTag 业务标识
     * @param paramName 请求参数名称
     * @return ES使用的key名称
     * */
    protected String getESName(String serviceTag, String paramName){
        String esName = properties.getProperty(paramName + "_" + serviceTag + ES_NAME);
        if(StringUtils.isEmpty(esName)){
            esName = paramName;
        }
        return esName;
    }


    /**
     * 判断是否nested结构查询,默认是非nested
     * @param esName 参数映射后的ES字段名
     * @param serviceTag
     * @return 是否为nested结构查询
     * */
    protected Boolean isNested(String esName, String serviceTag){
        if(esName.indexOf(NESTED_SPLIT_CHAR) != -1 && StringUtils.isNotEmpty(properties.getProperty(esName+SPLIT_CHAR+serviceTag+NESTED))){
            return Boolean.valueOf(properties.getProperty(esName+SPLIT_CHAR+serviceTag+NESTED));
        }
        return false;
    }

    /**
     * 根据规则，获取nestedPath
     * */
    protected String getNestedPath(String esName){
        return esName.substring(0, esName.lastIndexOf(NESTED_SPLIT_CHAR));
    }

    /**
     * 读取bool拼接关系， 默认是must,
     * must should
     * */
    protected int getMininumShouldMatch(String paramName, String serviceTag){
        String num = properties.getProperty(paramName + SPLIT_CHAR + serviceTag + MINIMUM_SHOULD_MATCH);
        if(StringUtils.isEmpty(num)){
            return 1;
        }
        return Integer.valueOf(num);
    }
}
