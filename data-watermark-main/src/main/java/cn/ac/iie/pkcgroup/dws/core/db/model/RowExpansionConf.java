package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class RowExpansionConf {
    @Value("${conf.forgeRowNumThreshold}")
    double forgeRowNumThreshold;
    @Value("${conf.rowCapacityLimit}")
    int rowCapacityLimit;
}
