package com.peter.search.util;

import com.peter.search.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * controller 增强器
 *
 * @author sam
 * @since 2018/4/8
 */
@ControllerAdvice
@Slf4j
public class ExceptionHandler {
    /**
     * 全局异常捕捉处理
     * @param ex
     * @return
     */
    @ResponseBody
    @org.springframework.web.bind.annotation.ExceptionHandler(value = {Exception.class,MethodArgumentNotValidException.class})
    public Result errorHandler(Exception ex) {
        ex.printStackTrace();
        log.error("程序异常:", ex);
        return Result.buildErr(ex.getMessage());
    }
}