package cn.ac.iie.pkcgroup.dws.comm.request.db;

import lombok.Data;

@Data
public class SelectedField {
    String fieldName;
    String algorithm;
    String fieldType; // only used for csv extraction
}
