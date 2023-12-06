package cn.ac.iie.pkcgroup.dws.comm.response.db;

import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

//@EqualsAndHashCode(callSuper = true)
//@AllArgsConstructor
//@Data
@SuperBuilder
public class DBInfoResponse extends BasicResponse {
    public ArrayList<DBInfo> dbInfoList;

//    public DBInfoResponse(int code, String msg) {
//        super(code, msg);
//    }

    @Data
    public static class DBInfo {
        private String dbName;
        private boolean isDefault;
    }
}
