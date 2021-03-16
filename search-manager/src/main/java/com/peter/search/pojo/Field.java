package com.peter.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class Field {

    private String columnName;
    private DataType dataType;
    private Boolean searchAble;
    private String analyzer;
    private String searchAnalyzer;
    private List<Field> fields;
}
