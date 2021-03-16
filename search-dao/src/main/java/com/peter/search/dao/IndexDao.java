package com.peter.search.dao;

import com.peter.search.dao.mapper.IndexMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.Index;
import com.peter.search.util.ExampleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IndexDao {


    @Autowired
    private IndexMapper indexMapper;

    public int insertOrUpdate(Index index){
        if(selectOne(index.getServiceTag()) != null){
            // update
            return updateIndex(index);
        } else {
            return indexMapper.insertSelective(index);
        }
    }

    public int updateIndex(Index index){
        Index where = new Index();
        where.setServiceTag(index.getServiceTag());
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return indexMapper.updateByExampleSelective(index, ExampleUtil.getCondition(where));
    }

    public Index selectOne(String serviceTag){
        Index index = new Index();
        index.setServiceTag(serviceTag);
        index.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return indexMapper.selectOne(index);
    }

    public int selectCount(String serviceTag){
        Index index = new Index();
        index.setServiceTag(serviceTag);
        index.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return indexMapper.selectCount(index);
    }

    public int deleteByServiceTag(String serviceTag){
        Index updateIndex = new Index();
        updateIndex.setLogicDelete(Constant.LOGIC_DELETE_YES);
        Index whereIndex = new Index();
        whereIndex.setServiceTag(serviceTag);
        whereIndex.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        return indexMapper.updateByExampleSelective(updateIndex, ExampleUtil.getCondition(whereIndex));
    }
}
