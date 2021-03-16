package com.peter.search.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class SuggestRequest {

    private String keyword;
    private JSONObject queryParam = new JSONObject();
    private Integer size = 10;
    private String serviceTag;

    public Integer getSize() {
        if(this.size > 20) return 20;
        return size;
    }
}
