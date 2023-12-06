package cn.ac.iie.pkcgroup.dws.comm.response.db;

import cn.ac.iie.pkcgroup.dws.comm.response.db.entity.AlgorithmInfo;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class AlgorithmListResponse extends BasicResponse {
    int algorithmCount;
    ArrayList<AlgorithmInfo> algorithmList;
}
