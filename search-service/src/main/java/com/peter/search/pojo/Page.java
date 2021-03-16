package com.peter.search.pojo;

import lombok.Data;

@Data
public class Page {

    private Integer totalCount;
    private Integer totalPage;
    private Integer currentPage;
    private Boolean isLastPage;
}
