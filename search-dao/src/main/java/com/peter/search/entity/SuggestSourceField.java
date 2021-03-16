package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_suggest_source_field")
@Data
public class SuggestSourceField extends BaseEntity {

    @Column(name="SERVICE_TAG",nullable=true)
    private String serviceTag;

    @Column(name="FILED_NAME",nullable=true)
    private String filedName;

    @Column(name="TYPE",nullable=true)
    private Integer type;
}
