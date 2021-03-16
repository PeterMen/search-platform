package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_service_tag")
@Data
public class ServiceTag extends BaseEntity{

    @Column(name="SERVICE_TAG",nullable=true)
    private String serviceTag;

    @Column(name="ES_NAME",nullable=true)
    private String esName;

    @Column(name="INDEX_ALIAS",nullable=true)
    private String indexAlias;

    @Column(name="TYPE_NAME",nullable=true)
    private String typeName;

    @Column(name="FULL_IMPORT_URL",nullable=true)
    private String fullImportUrl;
}
