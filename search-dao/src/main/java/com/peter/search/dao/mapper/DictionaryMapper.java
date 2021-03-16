package com.peter.search.dao.mapper;

import com.peter.search.entity.Dictionary;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

@Repository
public interface DictionaryMapper extends Mapper<Dictionary> {
}
