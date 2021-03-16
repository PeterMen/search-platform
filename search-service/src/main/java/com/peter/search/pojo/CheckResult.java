package com.peter.search.pojo;

import java.io.Serializable;

/**
 * 参数检查结果
 * 
 * @author 王海涛
 * @version 1.0
 *
 */
public class CheckResult implements Serializable {

    private static final long serialVersionUID = -30002886874112287L;

    /**
     * 1:成功;0失败
     */
    private boolean checkStatus;

    /**
     * 错误信息
     */
    private String errMsg;

    public CheckResult(boolean checkStatus){
        this.checkStatus = checkStatus;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public boolean getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(boolean checkStatus) {
        this.checkStatus = checkStatus;
    }
}
