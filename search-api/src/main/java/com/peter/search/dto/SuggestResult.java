package com.peter.search.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 返回结果集封装
 *
 * @author 王海涛
 * @version 1.0
 *
 */
public class SuggestResult implements Serializable {

    public SuggestResult(int status){
        this.status = status;
    }

    /**
     * 状态码 -- 失败
     */
    public static final int STATUS_FAILED = 0;

    /**
     * 状态码 -- 成功
     */
    public static final int STATUS_SUCCESS = 1;

    /**
     * 1:成功;0失败
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errMsg;

    private List<String> suggestWords;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public List<String> getSuggestWords() {
        return suggestWords;
    }

    public void setSuggestWords(List<String> suggestWords) {
        this.suggestWords = suggestWords;
    }
}
