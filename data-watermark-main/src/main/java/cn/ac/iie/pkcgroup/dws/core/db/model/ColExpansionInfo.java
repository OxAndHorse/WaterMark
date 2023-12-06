package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class ColExpansionInfo {
    String encoder;
    ArrayList<ColExpansionSubAlgorithm> subAlgorithms;

    @Data
    public static class ColExpansionSubAlgorithm {
        int subAlg;
        String fieldName;
        int fieldType;
    }
}
