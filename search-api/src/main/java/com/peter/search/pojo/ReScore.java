package com.peter.search.pojo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ReScore {

    private String reScoreId;
    private Map<String, Object> params;
    private Integer windowSize = 200;

    public Map<String, Object> getParams() {
        if(params == null){
            params = new HashMap<>(0);
        }
        return params;
    }
}
