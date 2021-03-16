package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_index_dictionary")
@Data
public class Dictionary extends BaseEntity {

    @Column(name="SERVICE_TAG",nullable=true)
    private String serviceTag;

    @Column(name="TYPE",nullable=true)
    private Integer type;

    @Column(name="IS_LOCAL_FILE",nullable=true)
    private Boolean isLocalFile;

    @Column(name="DIC_NAME",nullable=true)
    private String dicName;

    @Column(name="DIC_PATH",nullable=true)
    private String dicPath;
}
