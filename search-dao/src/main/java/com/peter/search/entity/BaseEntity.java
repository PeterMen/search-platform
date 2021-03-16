package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * 基础信息
 * @author lsh12724
 *
 */
@Data
public class BaseEntity implements Serializable{

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY,generator = "JDBC")
	private Long id;

	@Column(name="UPDATE_TIME",nullable=false)
	private String updateTime;
	
	@Column(name="CREATE_TIME",nullable=false)
    private String createTime;
	  
	@Column(name="LOGIC_DELETE",nullable=false)
    private Character logicDelete;

}
