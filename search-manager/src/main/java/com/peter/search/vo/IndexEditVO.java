package com.peter.search.vo;

import com.peter.search.pojo.DataType;
import lombok.Data;

import java.util.List;

@Data
public class IndexEditVO {
    private String serviceTag;
    private Integer shardingNum = 3;
    private Integer replicationNum = 1;
    private List<Column> columns;
    private List<EditType> editTypeList;

    @Data
    public class Column {
        private String columnName;
        private DataType dataType;
        private List<Column> columns;
    }

    /**
     * ES 支持的数据类型
     * */
    public enum EditType{
        /**不分词*/
        shard_edit("shard_edit"),
        replication_edit("replication_edit"),
        column_data_type_edit("column_data_type_edit"),
        column_add("column_add");

        private String editType;
        EditType(String editType){
            this.editType = editType;
        }
        public String getKey(){
            return this.editType;
        }
    }
}
