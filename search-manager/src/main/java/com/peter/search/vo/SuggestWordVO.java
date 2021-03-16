package com.peter.search.vo;

import lombok.Data;

import java.util.Map;

@Data
public class SuggestWordVO {

    private String serviceTag;
    private String suggestWord;
    private Map<String, String> conditionMap;
}
