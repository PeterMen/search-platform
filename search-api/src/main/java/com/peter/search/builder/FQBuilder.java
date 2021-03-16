package com.peter.search.builder;


import org.apache.commons.lang3.StringUtils;

public abstract class FQBuilder<T> extends BaseBuilder{


    public T fq(String key, Object value){
        if (blankCheck(value)) return (T) this;
        queryParam.put(key, value);
        return (T)this;
    }

    public T fq(String key, Object value, FQ_RULE rule){
        if (blankCheck(value)) return (T) this;
        fq(key, rule.getRule()+value);
        return (T)this;
    }

    public T fq(String key, Object value, String esName){
        if (blankCheck(value)) return (T) this;
        fq(key, value);
        setNameMapping(key, esName);
        return (T)this;
    }

    public T fq(String key, Object value, FQ_RULE rule, String esName){
        if (blankCheck(value)) return (T) this;
        fq(key, value, rule);
        setNameMapping(key, esName);
        return (T)this;
    }

    private boolean blankCheck(Object value) {
        if (value == null) return true;
        if (value instanceof String && StringUtils.isEmpty((String) value)) return true;
        return false;
    }

}
