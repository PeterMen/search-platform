package com.peter.search.dao;

import com.peter.search.dao.mapper.ServiceTagIndexMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.ServiceTagIndex;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServiceTagIndexDao {

    @Autowired
    private ServiceTagIndexMapper mapper;

    public int insert(String serviceTag, String indexName){
        ServiceTagIndex serviceTagIndex = new ServiceTagIndex();
        serviceTagIndex.setIndexName(indexName);
        serviceTagIndex.setServiceTag(serviceTag);
        return mapper.insertSelective(serviceTagIndex);
    }

    /**
     * delete oldest one if have 3 index
     * */
    @Transactional(rollbackFor = Exception.class)
    public List<String> deleteOldOne(String serviceTag){

        // find the oldest one
        Example example = new Example(ServiceTagIndex.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("serviceTag", serviceTag);
        example.setOrderByClause("create_time desc");
        List<ServiceTagIndex> indexList = mapper.selectByExampleAndRowBounds(example, new RowBounds(2, 50));

        if(!CollectionUtils.isEmpty(indexList)){
            // 删除索引
            for(ServiceTagIndex current : indexList){
                mapper.deleteByPrimaryKey(current);
            }
        }
        return indexList.stream().map(ServiceTagIndex::getIndexName).collect(Collectors.toList());
    }

    /**
     * delete all
     * */
    public List<String> deleteAll(String serviceTag){

        // select count
        ServiceTagIndex where = new ServiceTagIndex();
        where.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        where.setServiceTag(serviceTag);
        List<ServiceTagIndex> indexList = mapper.select(where);

        if(!CollectionUtils.isEmpty(indexList)){
            // 删除索引
            for(ServiceTagIndex current : indexList){
                mapper.deleteByPrimaryKey(current);
            }
        }
        return indexList.stream().map(ServiceTagIndex::getIndexName).collect(Collectors.toList());
    }
}
