package com.peter.search.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FullImportDTO {

    private String serviceTag;
    private String fullImportUrl;
    private Integer pageSize;
    private Boolean useFeign = true;
    private String newIndexName;
    private Map<String, String> extraParams;
}
