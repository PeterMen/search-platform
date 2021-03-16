package com.peter.search.dao;

import com.peter.search.dao.mapper.DictionaryMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DictionaryDao {

    @Autowired
    private DictionaryMapper dictionaryMapper;

    public List<Dictionary> getDictionaries(String serviceTag, Integer type){
        Dictionary dictionary = new Dictionary();
        dictionary.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        dictionary.setServiceTag(serviceTag);
        dictionary.setType(type);
        return dictionaryMapper.select(dictionary);
    }

    /**
     * 逻辑删除
     * */
    public int delDictionary(Long dicId){
        Dictionary dictionary = new Dictionary();
        dictionary.setId(dicId);
        dictionary.setLogicDelete(Constant.LOGIC_DELETE_YES);
        return dictionaryMapper.updateByPrimaryKeySelective(dictionary);
    }

    public Dictionary getDictionaryById(Long dicId){
        return dictionaryMapper.selectByPrimaryKey(dicId);
    }

    public int addDictionary(Dictionary dictionary){
        Dictionary dictionaryEntity = new Dictionary();
        dictionaryEntity.setDicName(dictionary.getDicName());
        dictionaryEntity.setDicPath(dictionary.getDicPath());
        dictionaryEntity.setIsLocalFile(dictionary.getIsLocalFile());
        dictionaryEntity.setServiceTag(dictionary.getServiceTag());
        dictionaryEntity.setType(dictionary.getType());
        return dictionaryMapper.insertSelective(dictionaryEntity);
    }

    public boolean exist(String serviceTag, String dicPath){
        Dictionary dictionaryEntity = new Dictionary();
        dictionaryEntity.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        dictionaryEntity.setServiceTag(serviceTag);
        dictionaryEntity.setDicPath(dicPath);
        return dictionaryMapper.selectCount(dictionaryEntity) > 0;
    }
}
