package com.peter.search.dao;

import com.peter.search.dao.mapper.DataDeleteTriggerMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.DeleteTrigger;
import com.peter.search.util.ExampleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class DataDeleteTriggerDao {

    @Autowired
    private DataDeleteTriggerMapper triggerMapper;

    public int insert(DeleteTrigger trigger){
        return triggerMapper.insertSelective(trigger);
    }

    public int updateTrigger(DeleteTrigger trigger){
        DeleteTrigger where = new DeleteTrigger();
        where.setServiceTag(trigger.getServiceTag());
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return triggerMapper.updateByExampleSelective(trigger, ExampleUtil.getCondition(where));
    }

    public int deleteTrigger(String serviceTag){
        DeleteTrigger update = new DeleteTrigger();
        update.setLogicDelete(Constant.LOGIC_DELETE_YES);
        DeleteTrigger where = new DeleteTrigger();
        where.setServiceTag(serviceTag);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return triggerMapper.updateByExampleSelective(update, ExampleUtil.getCondition(where));
    }

    public int deleteTriggerById(Long id){
        DeleteTrigger update = new DeleteTrigger();
        update.setLogicDelete(Constant.LOGIC_DELETE_YES);
        DeleteTrigger where = new DeleteTrigger();
        where.setId(id);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return triggerMapper.updateByExampleSelective(update, ExampleUtil.getCondition(where));
    }

    public List<DeleteTrigger> getTrigger(String serviceTag){
        DeleteTrigger where = new DeleteTrigger();
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        where.setServiceTag(serviceTag);
        return triggerMapper.select(where);
    }

    public List<DeleteTrigger> getTriggerList(){
        DeleteTrigger where = new DeleteTrigger();
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return triggerMapper.select(where);
    }

    public int updateLastTriggerTime(String serviceTag, Date nextTimePoint){
        DeleteTrigger update = new DeleteTrigger();
        update.setLastTriggerTime(nextTimePoint);
        DeleteTrigger where = new DeleteTrigger();
        where.setServiceTag(serviceTag);
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return triggerMapper.updateByExampleSelective(update, ExampleUtil.getCondition(where));
    }
}
