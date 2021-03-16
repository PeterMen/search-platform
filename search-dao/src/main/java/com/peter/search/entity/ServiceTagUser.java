package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_service_tag_user")
@Data
public class ServiceTagUser extends BaseEntity {

	@Column(name="SERVICE_TAG",nullable=true)
	private String serviceTag;

	@Column(name="USER",nullable=true)
	private String user;
}
