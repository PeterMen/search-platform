package com.peter.search.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.peter.search.dto.SearchRequestDTO;
import com.peter.search.pojo.CheckResult;
import com.peter.search.service.ParamCheckService;
import com.peter.search.service.check.CheckConst;
import com.peter.search.service.check.ParamCheck;
import com.peter.search.util.PropertyUtils;
import com.peter.search.util.WebAppContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 *  请求参数校验
 *
 * @author 七星
 * @date 2018年02月02号
 */
@Service
public class ParamCheckServiceImpl implements ParamCheckService {

    private static final String PARAM_CHECK_SUFFIX = "_PARAM_CHECK";

    private static final Logger logger = LoggerFactory.getLogger(ParamCheckServiceImpl.class);
    public static final String STATUS = "status";

    @Autowired
    PropertyUtils properties;

    /**
     * 参数校验
     * */
    @Override
    public CheckResult check(SearchRequestDTO requestParam) {

        CheckResult checkResult = new CheckResult(true);

        // 取数据
        String serviceTag = requestParam.getServiceTag();
        Object queryParam = requestParam.getQueryParam();

        try {

            // 公用参数校验
            if(queryParam instanceof JSONObject && CollectionUtils.isEmpty((JSONObject)queryParam)){
                checkResult.setCheckStatus(false);
                checkResult.setErrMsg("queryParam不能为空");
            } else if(queryParam instanceof JSONArray && CollectionUtils.isEmpty((JSONArray)queryParam)){
                checkResult.setErrMsg("queryParam不能为空");
                checkResult.setCheckStatus(false);
            } else if(StringUtils.isEmpty(serviceTag)){
                checkResult.setCheckStatus(false);
                checkResult.setErrMsg("serviceTag不能为空");
            } else {

                // serviceTag 值合法性校验
                boolean serviceTagCheck = false;
               String[] serviceTagArray = properties.getAllServiceTag();
                for(String curServiceTag : serviceTagArray){
                    if(StringUtils.equals(curServiceTag, serviceTag)){
                        serviceTagCheck = true;
                        break;
                    }
                }
                if(!serviceTagCheck){
                    checkResult.setCheckStatus(false);
                    checkResult.setErrMsg(serviceTag + "不支持");
                } else {

                    //参数校验
                    checkResult = checkRequestParams(serviceTag, queryParam);
                }
            }
            
        } catch (Exception e) {
            checkResult.setCheckStatus(false);
            checkResult.setErrMsg(e.getMessage());
            logger.error("查询参数检查失败：", e);
        }
        return checkResult;
    }

    /**
     * 校验请求参数params
     * @throws Exception 
     * 
     * */
    private CheckResult checkRequestParams(String serviceTag,  Object params){

        CheckResult checkResult = new CheckResult(true);
        try{
            
            // 如果未配置参数校验器，则不对参数做校验
            String paramCheckClass = properties.getProperty(serviceTag + PARAM_CHECK_SUFFIX);
            if(StringUtils.isEmpty(paramCheckClass)) {return checkResult;}
            
            // 校验参数是否合法
            ParamCheck paramCheck = (ParamCheck) WebAppContextUtil.getBean(paramCheckClass);
            
            // 判断检查结果
            Map<String, String> checkResultMap = paramCheck.check(params);
            if(CheckConst.STATUS_FAIL.equals(checkResultMap.get(STATUS))){
                checkResult.setCheckStatus(false);
                checkResult.setErrMsg(checkResultMap.get("errMsg"));
                return checkResult;
            }
        } catch (Exception e){
            checkResult.setCheckStatus(false);
            checkResult.setErrMsg(e.getMessage());
            logger.error("查询参数失败：", e);
            return checkResult;
        }
        return checkResult;
    }
}
