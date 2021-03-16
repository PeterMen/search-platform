package com.peter.search.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class SuggestWordDTO {

    private String serviceTag;
    private List<String> suggestFields;
    private List<String> conditionFields;

    public List<String> getConditionFields() {
        if(conditionFields == null) return Collections.emptyList();
        return conditionFields;
    }
}
