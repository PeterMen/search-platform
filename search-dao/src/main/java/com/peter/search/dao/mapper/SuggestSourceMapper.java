package com.peter.search.dao.mapper;

import com.peter.search.entity.SuggestSourceField;
import org.springframework.stereotype.Repository;
import tk.mybatis.mapper.common.Mapper;

@Repository
public interface SuggestSourceMapper extends Mapper<SuggestSourceField> {
}
