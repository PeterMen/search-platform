package com.peter.search.dao;

import com.peter.search.dao.mapper.IndexAnalyzerMapper;
import com.peter.search.entity.Constant;
import com.peter.search.entity.IndexAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IndexAnalyzerDao {

    @Autowired
    private IndexAnalyzerMapper indexAnalyzerMapper;

    public List<IndexAnalyzer> getAllAnalyzer(String serviceTag){
        IndexAnalyzer analyzerEntity = new IndexAnalyzer();
        analyzerEntity.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        analyzerEntity.setServiceTag(serviceTag);
        return indexAnalyzerMapper.select(analyzerEntity);
    }

    public int addAnalyzer(IndexAnalyzer analyzer){
        IndexAnalyzer analyzerEntity = new IndexAnalyzer();
        analyzerEntity.setName(analyzer.getName());
        analyzerEntity.setServiceTag(analyzer.getServiceTag());
        analyzerEntity.setTokenizer(analyzer.getTokenizer());
        analyzerEntity.setIntactDic(analyzer.getIntactDic());
        analyzerEntity.setSynonymDic(analyzer.getSynonymDic());
        analyzerEntity.setPinyinSearch(analyzer.getPinyinSearch());
        return indexAnalyzerMapper.insertSelective(analyzerEntity);
    }

    public boolean exist(IndexAnalyzer analyzer){
        IndexAnalyzer analyzerEntity = new IndexAnalyzer();
        analyzerEntity.setLogicDelete(Constant.LOGIC_DELETE_NOT);
        analyzerEntity.setServiceTag(analyzer.getServiceTag());
        analyzerEntity.setPinyinSearch(analyzer.getPinyinSearch());
        analyzerEntity.setTokenizer(analyzer.getTokenizer());
        analyzerEntity.setIntactDic(analyzer.getIntactDic());
        return indexAnalyzerMapper.selectCount(analyzerEntity) > 0;
    }

}
