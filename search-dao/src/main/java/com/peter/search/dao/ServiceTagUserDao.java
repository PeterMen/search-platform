package com.peter.search.dao;

import com.peter.search.dao.mapper.ServiceTagUserMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.ServiceTagUser;
import com.peter.search.util.ExampleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Component
public class ServiceTagUserDao {

    private static final String ALL_SERVICE_TAG = "all_service_tag";

    @Autowired
    private ServiceTagUserMapper serviceTagUserMapper;

    public boolean isAdminExist(String user){
        return isExist(ALL_SERVICE_TAG, user);
    }

    public boolean isExist(String serviceTag, String user){
        ServiceTagUser where = new ServiceTagUser();
        where.setUser(user);
        where.setServiceTag(serviceTag);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        int count = serviceTagUserMapper.selectCount(where);
        return count > 0 ? true : false;
    }

    public int insert(String serviceTag, String user){
        ServiceTagUser serviceTagUser = new ServiceTagUser();
        serviceTagUser.setServiceTag(serviceTag);
        serviceTagUser.setUser(user);
        return serviceTagUserMapper.insertSelective(serviceTagUser);
    }

    public int addAdmin(String user){
        ServiceTagUser serviceTagUser = new ServiceTagUser();
        serviceTagUser.setServiceTag(ALL_SERVICE_TAG);
        serviceTagUser.setUser(user);
        return serviceTagUserMapper.insertSelective(serviceTagUser);
    }

    public void deleteByServiceTag(String serviceTag){
        ServiceTagUser update = new ServiceTagUser();
        update.setLogicDelete(Constant.LOGIC_DELETE_YES);
        ServiceTagUser where = new ServiceTagUser();
        where.setServiceTag(serviceTag);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        serviceTagUserMapper.updateByExampleSelective(update, ExampleUtil.getCondition(where));
    }

    public List<String> getUserByServiceTag(String serviceTag){
        ServiceTagUser where = new ServiceTagUser();
        where.setServiceTag(serviceTag);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        List<ServiceTagUser> list = serviceTagUserMapper.select(where);
        if(CollectionUtils.isEmpty(list)) {
            return Collections.EMPTY_LIST;
        } else {
            return list.stream().map(ServiceTagUser::getUser).collect(Collectors.toList());
        }
    }

    public void deleteByUser(String serviceTag, String user){
        ServiceTagUser update = new ServiceTagUser();
        update.setLogicDelete(Constant.LOGIC_DELETE_YES);
        ServiceTagUser where = new ServiceTagUser();
        where.setUser(user);
        where.setServiceTag(serviceTag);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        serviceTagUserMapper.updateByExampleSelective(update, ExampleUtil.getCondition(where));
    }

    public List<String> getAllServiceTag(String user){
        ServiceTagUser where = new ServiceTagUser();
        where.setUser(user);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        List<ServiceTagUser> serviceTagUsers = serviceTagUserMapper.select(where);
        if(!CollectionUtils.isEmpty(serviceTagUsers)){
           for(ServiceTagUser serviceTagUser : serviceTagUsers){
               if(ALL_SERVICE_TAG.equals(serviceTagUser.getServiceTag())){
                   // 超级管理员
                   ServiceTagUser where2 = new ServiceTagUser();
                   where2.setLogicDelete(Constant.LOGIC_DELETE_NOT);
                   List<ServiceTagUser> serviceTags = serviceTagUserMapper.selectByExample(
                           ExampleUtil.getCondition(where2, d -> d.andNotEqualTo("serviceTag", ALL_SERVICE_TAG)));
                   return serviceTags.stream().map(ServiceTagUser::getServiceTag).distinct().collect(toList());
               }
           }
        }
        return serviceTagUsers.stream().map(ServiceTagUser::getServiceTag).distinct().collect(toList());
    }
}
