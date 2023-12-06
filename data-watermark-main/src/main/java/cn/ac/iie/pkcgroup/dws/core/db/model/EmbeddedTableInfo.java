package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Map;

@Data
public class EmbeddedTableInfo {
    String dbName;
    ArrayList<String> tables;
}
