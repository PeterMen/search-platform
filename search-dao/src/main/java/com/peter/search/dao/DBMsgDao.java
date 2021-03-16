package com.peter.search.dao;

import com.peter.search.dao.mapper.FailedMsgMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.FailedMsg;
import com.peter.search.util.ExampleUtil;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DBMsgDao {

    @Autowired
    private FailedMsgMapper failedMsgMapper;

    private static final String UNLOCKED = "-1";

    /**
     * 新插入失败消息
     * */
    public void saveFailedMsg(FailedMsg failedMsg){
        failedMsg.setRetryStatus(Constant.RETRY_YES);
        failedMsg.setLock(UNLOCKED);
        failedMsgMapper.insertSelective(failedMsg);
    }

    private String lockName = String.valueOf(System.currentTimeMillis());


    public List<FailedMsg> getFailedMsgList(){

        // lock data
        FailedMsg where = new FailedMsg();
        where.setRetryStatus(Constant.RETRY_YES);
        where.setLock(UNLOCKED);
        FailedMsg update = new FailedMsg();
        update.setLock(lockName);
        failedMsgMapper.updateByExampleSelective(update, ExampleUtil.getCondition(where));

        // get lock data
        where.setLock(lockName);
        where.setRetryStatus(Constant.RETRY_YES);
        List<FailedMsg> failedMsgs = failedMsgMapper.selectByRowBounds(where, new RowBounds(0, 20));

        // retrying
        if(!CollectionUtils.isEmpty(failedMsgs)){
            FailedMsg updateRetryIng = new FailedMsg();
            updateRetryIng.setRetryStatus(Constant.RETRY_ING);
            List<Long> lockedIdList = failedMsgs.stream().map(FailedMsg::getId).collect(Collectors.toList());
            failedMsgMapper.updateByExampleSelective(updateRetryIng,
                    ExampleUtil.getCondition(where, criteria -> criteria.andIn("id", lockedIdList)));
        }
        return failedMsgs;
    }

    /**
     * 消息再次消费失败后，更新失败原因，并设置消息消费不可重试
     * */
    public void msgFailedAgain(Long msgId, String errMsg){
        FailedMsg where = new FailedMsg();
        where.setId(msgId);

        FailedMsg update = new FailedMsg();
        update.setLock(UNLOCKED);
        update.setFailedReason(errMsg);
        update.setRetryStatus(Constant.RETRY_NO);
        failedMsgMapper.updateByExampleSelective(update, ExampleUtil.getCondition(where));
    }

    /**
     * 消息消费成功后，逻辑删除
     * */
    public void deleteSuccessMsg(Long msgId){
        // 物理删除
        failedMsgMapper.deleteByPrimaryKey(msgId);
    }

    public List<FailedMsg> getFailedMsgList(String serviceTag, Integer iStart, Integer iRowSize){

        FailedMsg where = new FailedMsg();
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        where.setServiceTag(serviceTag);
        Example ex = ExampleUtil.getCondition(where);
        ex.setOrderByClause("create_time desc");
        return failedMsgMapper.selectByExampleAndRowBounds(ex, new RowBounds(iStart, iRowSize));
    }

    public int updateFailedMsg(FailedMsg failedMsg){
        return failedMsgMapper.updateByPrimaryKeySelective(failedMsg);
    }
}
