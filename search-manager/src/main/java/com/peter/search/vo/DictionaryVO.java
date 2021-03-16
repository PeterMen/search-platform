package com.peter.search.vo;

import lombok.Data;

@Data
public class DictionaryVO {

    private String serviceTag;

    private Integer type;

    private Boolean isLocalFile;

    private String dicName;

    private String dicPath;
}
