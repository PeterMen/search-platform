package com.peter.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class Column {
    private String columnName;
    private DataType dataType;
    private Boolean searchAble;
    /** 当dataType为IK时，需要选择分析器id*/
    private Long analyzerId;
    private List<Column> columns;
}
