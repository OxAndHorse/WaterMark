package cn.ac.iie.pkcgroup.dws.data.table;

import cn.ac.iie.pkcgroup.dws.core.db.model.FieldModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.Map;

@Data
public class TableInfo {
    ArrayList<String> columnName;
    Map<String, FieldModel.FieldUnit> columnsInfo;
}
