package com.peter.search.dao;

import com.peter.search.dao.mapper.SuggestSourceMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.SuggestSourceField;
import com.peter.search.util.ExampleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SuggestSourceFieldDao {

    @Autowired
    private SuggestSourceMapper suggestSourceMapper;

    public List<SuggestSourceField> getAllField(String serviceTag){
        SuggestSourceField suggestSourceField = new SuggestSourceField();
        suggestSourceField.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        suggestSourceField.setServiceTag(serviceTag);
        return suggestSourceMapper.select(suggestSourceField);
    }

    public boolean existSuggestSourceField(String serviceTag, String field, Integer type){
        SuggestSourceField suggestSourceField = new SuggestSourceField();
        suggestSourceField.setFiledName(field);
        suggestSourceField.setServiceTag(serviceTag);
        suggestSourceField.setType(type);
        return suggestSourceMapper.selectCount(suggestSourceField) > 0;
    }

    public int addSuggestSourceField(String serviceTag, String field, Integer type){
        SuggestSourceField suggestSourceField = new SuggestSourceField();
        suggestSourceField.setFiledName(field);
        suggestSourceField.setServiceTag(serviceTag);
        suggestSourceField.setType(type);
        return suggestSourceMapper.insertSelective(suggestSourceField);
    }

    public int deletSuggestSourceField(String serviceTag, String field, Integer type){
        SuggestSourceField suggestSourceField = new SuggestSourceField();
        suggestSourceField.setFiledName(field);
        suggestSourceField.setServiceTag(serviceTag);
        suggestSourceField.setType(type);
        return suggestSourceMapper.deleteByExample(ExampleUtil.getCondition(suggestSourceField));
    }
}
