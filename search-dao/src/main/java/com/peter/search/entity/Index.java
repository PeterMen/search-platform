package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_index")
@Data
public class Index extends BaseEntity {

	@Column(name="SERVICE_TAG",nullable=true)
	private String serviceTag;

	@Column(name="SHARDING_NUM",nullable=true)
	private Integer shardingNum;

	@Column(name="REPLICATION_NUM",nullable=true)
    private Integer replicationNum;

	@Column(name="MAPPING",nullable=true)
    private String mapping;
}
