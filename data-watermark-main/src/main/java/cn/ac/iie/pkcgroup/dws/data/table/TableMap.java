package cn.ac.iie.pkcgroup.dws.data.table;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TableMap {
    Map<String, TableInfoWithPK> infoMap = new HashMap<>(4);
}
