package cn.ac.iie.pkcgroup.dws.comm.response.app;

import cn.ac.iie.pkcgroup.dws.comm.response.app.entity.DBMetaData;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class DBListResponse extends BasicResponse {
    int dbCount;
    ArrayList<DBMetaData> dbInfoList;
}
