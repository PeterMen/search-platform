package com.peter.search.dto;

import com.peter.search.pojo.DocData;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateRequestDTO {

    private String serviceTag;
    private OP_TYPE opType;
    private List<DocData> docDataList;

    public List<DocData> getDocDataList() {
        if(docDataList == null){
            docDataList = new ArrayList<>();
        }
        return docDataList;
    }

}
