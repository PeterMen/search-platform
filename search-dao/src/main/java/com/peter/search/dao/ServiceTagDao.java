package com.peter.search.dao;

import com.peter.search.dao.mapper.ServiceTagMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.ServiceTag;
import com.peter.search.util.ExampleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceTagDao {

    @Autowired
    private ServiceTagMapper serviceTagMapper;

    public List<ServiceTag> getAllServiceTag(){
        ServiceTag serviceTag = new ServiceTag();
        serviceTag.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return serviceTagMapper.select(serviceTag);
    }

    public ServiceTag getServiceTag(String serviceTagStr){
        ServiceTag serviceTag = new ServiceTag();
        // 唯一主键是indexAlias或者serviceTag
        serviceTag.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return serviceTagMapper.selectOneByExample(
                ExampleUtil.getCondition(serviceTag,
                        criteria ->
                                criteria.andCondition("(INDEX_ALIAS='" + serviceTagStr.toLowerCase()
                                        + "' or SERVICE_TAG='" + serviceTagStr + "')")
                        ));
    }

    public int createServiceTag(ServiceTag serviceTag){
        return serviceTagMapper.insertSelective(serviceTag);
    }

    public int updateServiceTag(ServiceTag serviceTag){
        ServiceTag serviceTagWhere = new ServiceTag();
        serviceTagWhere.setServiceTag(serviceTag.getServiceTag());
        serviceTagWhere.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return serviceTagMapper.updateByExampleSelective(serviceTag, ExampleUtil.getCondition(serviceTagWhere));
    }

    public void deleteServiceTag(String serviceTag){
        ServiceTag update = new ServiceTag();
        update.setLogicDelete(Constant.LOGIC_DELETE_YES);
        ServiceTag where = new ServiceTag();
        where.setServiceTag(serviceTag);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        serviceTagMapper.updateByExampleSelective(update, ExampleUtil.getCondition(where));
    }
}
