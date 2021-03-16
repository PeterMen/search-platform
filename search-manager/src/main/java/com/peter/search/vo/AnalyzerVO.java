package com.peter.search.vo;

import lombok.Data;

@Data
public class AnalyzerVO {
    private String serviceTag;
    private String name;
    private String tokenizer;
    private boolean pinyinSearch;
    private String intactDic;
    private String synonymDic;
}
