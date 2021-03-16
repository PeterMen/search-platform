package com.peter.search.util;

import com.peter.search.dao.ServiceTagDao;
import com.peter.search.entity.ServiceTag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  属性文件工具类
 * @author 王海涛
 */
@Component
public class PropertyUtils{

    private static HashMap<String, String> PROPERTIES = new HashMap<>(64);
    public static final ThreadLocal<Map<String, String>> SEARCH_CONFIG = new ThreadLocal<>();
    private static String[] allServiceTag = new String[0];

    @Autowired
    Environment environment;

    @Autowired
    ServiceTagDao serviceTagDao;

    public String getProperty(String key){
        String value = SEARCH_CONFIG.get() != null
                ? SEARCH_CONFIG.get().get(key) : null;
        if(StringUtils.isEmpty(value)){
            value = environment.getProperty(key);
        }
        return value;
    }

    public String getProperty(String key, String defaultVal){
        String value = getProperty(key);
        if(StringUtils.isEmpty(value)){
            value = defaultVal;
        }
        return value;
    }

    public String getESName(String serviceTag){
        return getDbProperty(serviceTag + Constant.ES);
    }
    public String getIndexAlias(String serviceTag){
        return getDbProperty(serviceTag + Constant.INDEX_ALIAS);
    }
    public String getTypeName(String serviceTag){
        return getDbProperty(serviceTag + Constant.TYPE);
    }
    public String getFullImportUrl(String serviceTag){
        return getDbProperty(serviceTag + Constant.FULL_DATA_IMPORT_URL);
    }
    public String[] getAllServiceTag(){
        if(allServiceTag.length == 0) {
            // 未初始化
            loadAllServiceTag();
        }
        return allServiceTag;
    }

    private String getDbProperty(String key) {
        String value = PROPERTIES.get(key);
        if(StringUtils.isEmpty(value)){
            synchronized (PROPERTIES){
                value = PROPERTIES.get(key);
                if(StringUtils.isEmpty(value)){
                    loadAllServiceTag();
                    value = PROPERTIES.get(key);
                }
            }
        }
        return value;
    }

    public void loadAllServiceTag() {
        List<ServiceTag> tagList = serviceTagDao.getAllServiceTag();
        String[] allST = new String[tagList.size()];
        if(!CollectionUtils.isEmpty(tagList)){
            int index = 0;
            for(ServiceTag tag : tagList){
                PROPERTIES.put(tag.getServiceTag()+Constant.ES, tag.getEsName());
                PROPERTIES.put(tag.getServiceTag()+Constant.INDEX_ALIAS, tag.getIndexAlias());
                PROPERTIES.put(tag.getServiceTag()+Constant.TYPE, tag.getTypeName());
                PROPERTIES.put(tag.getServiceTag()+Constant.FULL_DATA_IMPORT_URL, tag.getFullImportUrl());
                allST[index++] = tag.getServiceTag();
            }
            synchronized (allServiceTag){
                allServiceTag = allST;
            }
        }
    }
}

