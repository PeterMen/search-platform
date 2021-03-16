package com.peter.search.service.check;

import java.util.Map;

/**
 * 参数校验接口
 *
 * @author 王海涛
 * */
public interface ParamCheck {

    /**
     * 参数校验方法
     * 
     * @param json
     * @return status:校验结果 1-成功 0-失败
     *         errMsg:失败提示信息
     * */
    Map<String, String> check(Object json);
}
