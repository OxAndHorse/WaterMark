package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RowExpansionInfo {
    String algorithm;
    double threshold;
    int maxCapacity;
}
