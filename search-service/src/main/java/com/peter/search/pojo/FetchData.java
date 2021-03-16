package com.peter.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class FetchData {
    private Integer status;
    private Page page;
    private List<DocData> dataList;
}
