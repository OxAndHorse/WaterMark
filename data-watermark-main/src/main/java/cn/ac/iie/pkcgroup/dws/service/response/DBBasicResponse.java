package cn.ac.iie.pkcgroup.dws.service.response;

import cn.ac.iie.pkcgroup.dws.comm.response.db.entity.SimpleTableInfo;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class DBBasicResponse extends BasicResponse {
    ArrayList<SimpleTableInfo> simpleTableInfos;
    ArrayList<ArrayList<String>> pagedDataSet;
    int totalCount;
}
