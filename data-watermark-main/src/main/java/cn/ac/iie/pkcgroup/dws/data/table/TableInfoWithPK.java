package cn.ac.iie.pkcgroup.dws.data.table;

import lombok.Data;

import java.util.ArrayList;

@Data
public class TableInfoWithPK {
    TableInfo tableInfo;
    String primaryKey;
    ArrayList<String> primaryKeyList;
    ArrayList<Integer> pkIndex;
}
