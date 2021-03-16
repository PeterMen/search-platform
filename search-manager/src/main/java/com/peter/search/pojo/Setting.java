package com.peter.search.pojo;

import com.peter.search.entity.IndexAnalyzer;
import com.peter.search.util.SettingUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 需要从页面参数传过来的column参数
 * 设置pinyinSearch和ext_dic_main两个字段
 * 才可以使用getSetting方法
 * */
@Data
public class Setting {

    public static final String search_analyzer_ik_smart = "ik_smart";
    public static final String search_analyzer_ik_smart_py = "ik_smart_py";
    public static final String search_analyzer_ik_smart_py_cd = "ik_smart_py_cd";
    public static final String search_analyzer_ik_smart_cd = "ik_smart_cd";
    public static final String analyzer_ik_max_word = "ik_max_word";
    public static final String analyzer_ik_max_word_py = "ik_max_word_py";
    public static final String analyzer_ik_max_word_py_cd = "ik_max_word_py_cd";
    public static final String analyzer_ik_max_word_cd = "ik_max_word_cd";
    /** 加同义词 */
    public static final String search_analyzer_ik_smart_synonym = "ik_smart_sy";
    public static final String search_analyzer_ik_smart_py_synonym = "ik_smart_py_sy";
    public static final String search_analyzer_ik_smart_py_cd_synonym = "ik_smart_py_cd_sy";
    public static final String search_analyzer_ik_smart_cd_synonym = "ik_smart_cd_sy";
    public static final String analyzer_ik_max_word_synonym = "ik_max_word_sy";
    public static final String analyzer_ik_max_word_py_synonym = "ik_max_word_py_sy";
    public static final String analyzer_ik_max_word_py_cd_synonym = "ik_max_word_py_cd_sy";
    public static final String analyzer_ik_max_word_cd_synonym = "ik_max_word_cd_sy";

    public static final String pinyin_filter =
            "                \"pinyin_filter\" : {\n" +
            "                    \"type\" : \"pinyin\",\n" +
            "                    \"keep_joined_full_pinyin\" : true,\n" +
            "                    \"keep_first_letter\" : true,\n" +
            "                    \"keep_full_pinyin\" : false,\n" +
            "                    \"keep_none_chinese\" : true,\n" +
            "                    \"keep_original\" : true,\n" +
            "                    \"limit_first_letter_length\" : 16,\n" +
            "                    \"lowercase\" : true,\n" +
            "                    \"trim_whitespace\" : true,\n" +
            "                    \"keep_none_chinese_in_first_letter\" : true\n" +
            "                }\n" ;

    private static final String synonym_filter_start =
            "   \"synonym_filter\": {\n" +
                    "\"ignore_case\": \"true\",\n" +
                    "\"type\": \"dynamic_synonym\",\n" +
                    "\"synonyms_path\": [\n\"";
    private static final String synonym_filter_end = "\"]}";

    private Integer shardingNum;
    private Integer replicationNum;
    private List<IndexAnalyzer> usedAnalyzers;

    private String filter(){

        // 未用到任何自定义分析器
        if(CollectionUtils.isEmpty(usedAnalyzers)) return  "";

        // 是否使用拼音
        boolean usePinyin = usedAnalyzers.stream().anyMatch(IndexAnalyzer::getPinyinSearch);

        // 是否使用同义词
        List<String> synonymDics = usedAnalyzers.stream()
                .filter(r -> StringUtils.isNotEmpty(r.getSynonymDic()))
                .map(IndexAnalyzer::getSynonymDic)
                .collect(Collectors.toList());
        String synonym_filter = synonym_filter_start + StringUtils.join(synonymDics, "\",\"") + synonym_filter_end;

        if(usePinyin && !CollectionUtils.isEmpty(synonymDics)){
            return pinyin_filter + "," + synonym_filter;
        } else if(usePinyin) {
            return pinyin_filter;
        } else if(!CollectionUtils.isEmpty(synonymDics)) {
            return synonym_filter;
        } else {
            return "";
        }
    }

    private String tokenizer(){
        List<String> tokenDics = usedAnalyzers.stream()
                .filter(r -> StringUtils.isNotEmpty(r.getIntactDic()))
                .map(IndexAnalyzer::getIntactDic)
                .collect(Collectors.toList());

        if(!CollectionUtils.isEmpty(tokenDics)){
            String dics = StringUtils.join(tokenDics, "\",\"");
            return "                \"tk_max_word\":{\n" +
                    "                    \"type\":\"ik_max_word\",\n" +
                    "                    \"ext_dic_main\":[\""+ dics +"\"]\n" +
                    "                },\n" +
                    "                \"tk_smart\":{\n" +
                    "                    \"type\":\"ik_smart\",\n" +
                    "                    \"ext_dic_main\":[\""+ dics +"\"]\n" +
                    "                }\n";
        } else {
            return "";
        }
    }

    /**
     * 拼接所有用到的analyzer分析器
     * */
    private String analyzers(){
        HashSet<String> analyzerName = new HashSet();
        List<String> analyzers = usedAnalyzers.stream()
                .map(r -> {
                    StringBuilder analyzer= new StringBuilder();
                    if(analyzerName.add(SettingUtil.getAnalyzerName(r))){
                        analyzer.append(analyzer(r, false));
                    }
                    if(analyzerName.add(SettingUtil.getSearchAnalyzerName(r))){
                        if(analyzer.length() != 0) analyzer.append(",");
                        analyzer.append(analyzer(r, true));
                    }
                    return analyzer.toString();
                }).filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
        return StringUtils.join(analyzers, ",");
    }

    private String analyzer(IndexAnalyzer analyzer, boolean isSearch){
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        sb.append(isSearch ? SettingUtil.getSearchAnalyzerName(analyzer) :SettingUtil.getAnalyzerName(analyzer));
        sb.append("\":{\"type\":\"custom\",\"tokenizer\":\"");
        sb.append(tokenizerName(analyzer, isSearch));
        sb.append("\",\"filter\":[");
        boolean needPinYinFilter = analyzer.getPinyinSearch()&&(!isSearch);
        boolean needSynonym = StringUtils.isNotEmpty(analyzer.getSynonymDic()) && isSearch;
        sb.append(needPinYinFilter?"\"pinyin_filter\"":"");
        sb.append(needSynonym ? "\"synonym_filter\"":"");
        sb.append("]}");
        return sb.toString();
    }

    private String tokenizerName(IndexAnalyzer analyzer, boolean isSearch){
        if(StringUtils.isEmpty(analyzer.getIntactDic())){
            return isSearch ? "ik_smart" : "ik_max_word";
        } else {
            return isSearch ? "tk_smart" : "tk_max_word";
        }
    }

    /**
     * pinyinSearch 和 ext_dic_main.size()有四种组合
     * 分别产生四种analyzer：(ik_smart,ik_max_word),(ik_smart_py,ik_max_word_py),
     * (ik_smart_py_cd,ik_max_word_py_cd),(ik_smart_cd,ik_max_word_cd)
     * */
    public String getSetting() {

        return "{\n" +
                "    \"settings\" : {\n" +
                "        \"number_of_shards\" : " +shardingNum+",\n" +
                "        \"number_of_replicas\" : " +replicationNum+",\n" +
                "\t\t\"analysis\":{\n" +
                "            \"filter\" : {" + filter() + " }," +
                "            \"tokenizer\":{" + tokenizer() + " }," +
                "            \"analyzer\":{" + analyzers() + " }" +
                "        }\n" +
                "    }\n" +
                "}";
    }

}
