package com.peter.search.dto;

import lombok.Data;

@Data
public class Result {

    private Integer status;
    private Object data;
    private String message;

    public static Result buildSuccess(){
        Result rs = new Result();
        rs.setStatus(1);
        return rs;
    }
    public static Result buildSuccess(Object data){
        Result rs = new Result();
        rs.setStatus(1);
        rs.setData(data);
        return rs;
    }
    public static Result buildSuccess(String msg){
        Result rs = new Result();
        rs.setStatus(1);
        rs.setMessage(msg);
        return rs;
    }

    public static Result buildErr(String errMsg){
        Result rs = new Result();
        rs.setStatus(0);
        rs.setMessage(errMsg);
        return rs;
    }
}
