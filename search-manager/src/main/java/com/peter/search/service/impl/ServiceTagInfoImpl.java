package com.peter.search.service.impl;

import com.peter.search.dao.ServiceTagDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceTagInfoImpl {

    @Autowired
    private ServiceTagDao dao;

    public String getIndexAlias(String serviceTag){
        return dao.getServiceTag(serviceTag).getIndexAlias();
    }

    public String getTypeName(String serviceTag){
        return dao.getServiceTag(serviceTag).getTypeName();
    }

    public String getEsName(String serviceTag){
        return dao.getServiceTag(serviceTag).getEsName();
    }

    public String getDataSourceURL(String serviceTag){
        return dao.getServiceTag(serviceTag).getFullImportUrl();
    }
}
