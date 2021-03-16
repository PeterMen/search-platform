package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_failed_msg")
@Data
public class FailedMsg extends BaseEntity {

	@Column(name="SERVICE_TAG",nullable=true)
	private String serviceTag;

	@Column(name="OP_TYPE",nullable=true)
	private String opType;

	/** 1-可重试，0-不可重试 */
	@Column(name="RETRY_STATUS",nullable=true)
    private Character retryStatus;

	@Column(name="MSG_CONTENT",nullable=true)
    private String msgContent;

	@Column(name="FAILED_REASON",nullable=true)
    private String failedReason;

	@Column(name="DATA_LOCK",nullable=true)
	private String lock;
}
