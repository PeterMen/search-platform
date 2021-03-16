package com.peter.search.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Table(name="t_score_model")
@Data
public class ScoreModel extends BaseEntity {

    @Column(name="SERVICE_TAG",nullable=true)
    private String serviceTag;

    @Column(name="NAME",nullable=true)
    private String name;

    @Column(name="MODEL_TYPE",nullable=true)
    private Integer modelType;

    @Column(name="MODEL_CONTENT",nullable=true)
    private String modelContent;
}
