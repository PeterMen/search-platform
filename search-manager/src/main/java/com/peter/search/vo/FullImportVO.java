package com.peter.search.vo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Data
public class FullImportVO {

    private String serviceTag;
    private String fullImportUrl;
    private Integer pageSize;
    private String extraParams;
    private Boolean useFeign = true;

    public Map<String, String> getExtraParams() {
        Map<String, String> resultMap = new HashMap<>(4);
        if(StringUtils.isNotEmpty(extraParams)){
            for(String param : extraParams.split("&")){
                if(StringUtils.isNotEmpty(param)){
                    String[] paramArray = param.split("=");
                    if(paramArray.length == 2){
                        resultMap.put(paramArray[0], paramArray[1]);
                    }
                }
            }
        }
        return resultMap;
    }
}
