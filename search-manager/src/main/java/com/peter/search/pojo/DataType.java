package com.peter.search.pojo;

/**
 * ES 支持的数据类型
 * */
public enum DataType{
    /**不分词*/
    KEYWORD("keyword"),
    /** searchAble must be true*/
    TEXT_IK_SEARCH("text_ik_search"),
    /** searchAble must be true*/
    TEXT_STANDARD_SEARCH("text_standard_search"),
    /**基本数据类型*/
    LONG("long"),
    INTEGER("integer"),
    SHORT("short"),
    BYTE("byte"),
    DOUBLE("double"),
    FLOAT("float"),
    DATE("date"),
    BOOLEAN("boolean"),
    OBJECT("object"),
    NESTED("nested"),
    GEO_POINT("geo_point"),
    GEO_SHAPE("geo_shape"),
    ENGLISH("english"),
    IP("ip");


    private String dataType;
    DataType(String dataType){
        this.dataType = dataType;
    }
    public String getKey(){
        return this.dataType;
    }
}
