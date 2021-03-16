package com.peter.search.builder;

/**
 * 所有可用的查询解析器名称
 * */
public enum FQ_RULE{
    GT("gt:"), LT("lt:"), GTE("gte:"), LTE("lte:"), OR("or:"), WILDCARD("wildcard:"),
    AND("and:"), RA("ra:"), RAL("ral:"), RAR("rar:"), PREFIX("prefix:"), NOT("!:"),
    NOT_GT("!gt:"), NOT_LT("!lt:"), NOT_GTE("!gte:"), NOT_LTE("!lte:"), NOT_OR("!or:"), NOT_WILDCARD("!wildcard:"),
    NOT_AND("!and:"), NOT_RA("!ra:"), NOT_RAL("!ral:"), NOT_RAR("!rar:"), NOT_PREFIX("!prefix:");

    private String rule;
    FQ_RULE(String rule){
        this.rule = rule;
    }
    public String getRule(){
        return this.rule;
    }
}