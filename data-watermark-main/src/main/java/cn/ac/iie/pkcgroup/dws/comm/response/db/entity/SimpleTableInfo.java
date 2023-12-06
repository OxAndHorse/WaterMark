package cn.ac.iie.pkcgroup.dws.comm.response.db.entity;

import lombok.Data;

import java.util.ArrayList;

@Data
public class SimpleTableInfo {
    String tableName;
    int tableFieldCount;
    ArrayList<TableField> tableFields;
    boolean isDefault;

    @Data
    public static class TableField {
        String fieldName;
        String fieldType;
        boolean isPrimaryKey;
    }
}
