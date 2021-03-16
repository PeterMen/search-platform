package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_service_tag_index")
@Data
public class ServiceTagIndex extends BaseEntity {

	@Column(name="SERVICE_TAG",nullable=true)
	private String serviceTag;

	@Column(name="INDEX_NAME",nullable=true)
	private String indexName;
}
