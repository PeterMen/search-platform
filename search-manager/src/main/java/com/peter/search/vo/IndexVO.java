package com.peter.search.vo;

import com.peter.search.pojo.Column;
import lombok.Data;

import java.util.List;

@Data
public class IndexVO {
    private String serviceTag;
    private Integer shardingNum = 3;
    private Integer replicationNum = 1;
    private List<Column> columns;
}
