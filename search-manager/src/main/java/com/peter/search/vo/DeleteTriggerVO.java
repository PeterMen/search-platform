package com.peter.search.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class DeleteTriggerVO{

    @NotEmpty(message="serviceTag不能为空")
    private String serviceTag;

    @NotEmpty(message="deleteJson不能为空")
    private String deleteJson;

    private Integer triggerType;
    private String triggerTime;
}
