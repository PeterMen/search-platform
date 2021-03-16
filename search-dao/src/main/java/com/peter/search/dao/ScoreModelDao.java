package com.peter.search.dao;

import com.peter.search.dao.mapper.ScoreModelMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.ScoreModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoreModelDao {

    @Autowired
    private ScoreModelMapper scoreModelMapper;

    public List<ScoreModel> getAllScoreModel(String serviceTag){
        ScoreModel scoreModel = new ScoreModel();
        scoreModel.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        scoreModel.setServiceTag(serviceTag);
        return scoreModelMapper.select(scoreModel);
    }

    public ScoreModel getScoreModelByID(Long modelId){
        return scoreModelMapper.selectByPrimaryKey(modelId);
    }

    /**
     * 逻辑删除
     * */
    public int delScoreModel(Long id){
        ScoreModel scoreModel = new ScoreModel();
        scoreModel.setId(id);
        scoreModel.setLogicDelete(Constant.LOGIC_DELETE_YES);
        return scoreModelMapper.updateByPrimaryKeySelective(scoreModel);
    }

    public Long createModel(String serviceTag, String modelName,String modelContent){
        ScoreModel scoreModel = new ScoreModel();
        scoreModel.setServiceTag(serviceTag);
        scoreModel.setModelType(1);
        scoreModel.setModelContent(modelContent);
        scoreModel.setName(modelName);
        scoreModelMapper.insertSelective(scoreModel);
        return scoreModel.getId();
    }

    public int updateModel(Long id, String modelName,String modelContent){
        ScoreModel scoreModel = new ScoreModel();
        scoreModel.setId(id);
        scoreModel.setModelContent(modelContent);
        scoreModel.setName(modelName);
        return scoreModelMapper.updateByPrimaryKeySelective(scoreModel);
    }
}
