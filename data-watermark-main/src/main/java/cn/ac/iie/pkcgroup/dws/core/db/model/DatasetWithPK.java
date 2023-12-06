package cn.ac.iie.pkcgroup.dws.core.db.model;

import cn.ac.iie.pkcgroup.dws.comm.request.db.SelectedField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.*;
import java.util.*;

/**
 * 带有主键的数据集，用于划分
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class DatasetWithPK extends Dataset {
    // key: primary key, value: row data
    private Map<String, ArrayList<String>> dataset;
    private ArrayList<FieldModel.FieldUnit> metaData;
    private int sortedIndex;

    public DatasetWithPK() {
        dataset = new HashMap<>();
    }
}


