package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class FieldModel {
    FieldUnit selectedField;
    ArrayList<FieldUnit> primaryKeys;

    @Data
    public static class FieldUnit {
        private String fieldName;
        private int fieldIndex;
        private String fieldType;
        private int fieldTypeCode;
        private int fieldSize;
        private boolean isPrimaryKey;
        private boolean isAutoIncrement;
    }
}
