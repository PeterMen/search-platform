package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_index_analyzer")
@Data
public class IndexAnalyzer extends BaseEntity {

    @Column(name="SERVICE_TAG",nullable=true)
    private String serviceTag;

    @Column(name="NAME",nullable=true)
    private String name;

    @Column(name="TOKENIZER",nullable=true)
    private String tokenizer;

    @Column(name="PINYIN_SEARCH",nullable=true)
    private Boolean pinyinSearch;

    @Column(name="INTACT_DIC",nullable=true)
    private String intactDic;

    @Column(name="SYNONYM_DIC",nullable=true)
    private String synonymDic;

}
