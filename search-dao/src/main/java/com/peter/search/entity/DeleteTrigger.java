package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;
import java.util.Date;

@Table(name="t_index_data_delete_trigger")
@Data
public class DeleteTrigger extends BaseEntity {

    @Column(name="SERVICE_TAG",nullable=true)
    private String serviceTag;

    @Column(name="DELETE_JSON",nullable=true)
    private String deleteJson;

    @Column(name="TRIGGER_TYPE",nullable=true)
    private Integer triggerType;

    @Column(name="TRIGGER_TIME",nullable=true)
    private String triggerTime;

    @Column(name="LAST_TRIGGER_TIME",nullable=true)
    private Date lastTriggerTime;

}
