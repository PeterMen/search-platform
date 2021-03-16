package com.peter.search.util;

import com.peter.search.entity.IndexAnalyzer;
import com.peter.search.pojo.Column;
import com.peter.search.pojo.DataType;
import com.peter.search.pojo.Field;
import com.peter.search.pojo.Setting;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingUtil {

    /**
     * 根据自定义表结构格式转换成ES的mapping
     * */
    public static List<Field> transferToField(List<Column> columns, Map<Long, IndexAnalyzer> analyzerMap){

        List<Field> fields = new ArrayList<>();
        for(Column c : columns){
            Field f = new Field();
            f.setDataType(c.getDataType());
            f.setColumnName(c.getColumnName());
            f.setSearchAble(c.getSearchAble());
            if(c.getDataType() == DataType.OBJECT || c.getDataType() == DataType.NESTED){
                f.setFields(transferToField(c.getColumns(), analyzerMap));
            } else if(c.getDataType() == DataType.TEXT_IK_SEARCH){
                // 设置分析器analyzer
                if(c.getAnalyzerId() == null || analyzerMap.get(c.getAnalyzerId()) == null){
                    f.setAnalyzer(Setting.analyzer_ik_max_word);
                    f.setSearchAnalyzer(Setting.search_analyzer_ik_smart);
                } else {
                    f.setAnalyzer(getAnalyzerName(analyzerMap.get(c.getAnalyzerId())));
                    f.setSearchAnalyzer(getSearchAnalyzerName(analyzerMap.get(c.getAnalyzerId())));
                }
            }
            fields.add(f);
        }
        return fields;
    }

    /**
     * 根据配置的分析器生成analyzer名称
     * */
    public static String getAnalyzerName(IndexAnalyzer analyzer){
        if(StringUtils.isEmpty(analyzer.getIntactDic())){
            // 无自定义字典
            if(analyzer.getPinyinSearch()){
                // 要拼音 无同义词
                return Setting.analyzer_ik_max_word_py;
            } else {
                // 不要拼音 无同义词
                return Setting.analyzer_ik_max_word_cd;
            }
        } else {
            // 有自定义字典
            if(analyzer.getPinyinSearch()){
                // 要拼音 无同义词
                return Setting.analyzer_ik_max_word_py_cd;
            } else {
                // 不要拼音 无同义词
                return Setting.analyzer_ik_max_word_cd;
            }
        }
    }

    /**
     * 根据配置的分析器生成 search analyzer名称
     * */
    public static String getSearchAnalyzerName(IndexAnalyzer analyzer){
        if(StringUtils.isEmpty(analyzer.getIntactDic())){
            // 查询时，不要拼音filter,否则，查询“黑色”会把“红色也查出来”
            if(StringUtils.isEmpty(analyzer.getSynonymDic())){
                // 无同义词
                return Setting.search_analyzer_ik_smart_cd;
            } else {
                // 要同义词
                return Setting.search_analyzer_ik_smart_synonym;
            }
        } else {
            // 有自定义字典
            // 不要拼音
            if(StringUtils.isEmpty(analyzer.getSynonymDic())){
                // 无同义词
                return Setting.search_analyzer_ik_smart_cd;
            } else {
                // 要同义词
                return Setting.search_analyzer_ik_smart_cd_synonym;
            }
        }
    }
}
