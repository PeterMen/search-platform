package com.peter.search.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 返回结果集封装
 * 
 * @author 王海涛
 * @version 1.0
 *
 */
public class MSearchResultDTO implements Serializable {

    public MSearchResultDTO(){}
    private static final long serialVersionUID = -30002886874112287L;

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

    /**
     * 相应时间
     */
    private Long serviceRespTimeMillis;

    private List<SearchResultDTO> data;

    public List<SearchResultDTO> getData() {
        if(data == null){
            data = new ArrayList<>();
        }
        return data;
    }

    public void setData(List<SearchResultDTO> data) {
        this.data = data;
    }

    public Integer getStatus() {
      return status;
    }

    public void setStatus(Integer status) {
      this.status = status;
    }

    public MSearchResultDTO(int status) {
      this.status = status;
    }

    public Long getServiceRespTimeMillis() {
        return serviceRespTimeMillis;
    }

    public void setServiceRespTimeMillis(Long serviceRespTimeMillis) {
        this.serviceRespTimeMillis = serviceRespTimeMillis;
    }

    public static int getStatusFailed() {
        return STATUS_FAILED;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
