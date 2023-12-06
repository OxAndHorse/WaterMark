package cn.ac.iie.pkcgroup.dws.handler.app.response;

import cn.ac.iie.pkcgroup.dws.comm.response.app.entity.AppMetaData;
import cn.ac.iie.pkcgroup.dws.comm.response.app.entity.AppSimpleInfo;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class AppResponse extends BasicResponse {
    AppMetaData appMetaData;
    ArrayList<String> dbList;
    ArrayList<AppSimpleInfo> appSimpleInfos;
    public AppResponse(int code, String msg) {
        super(code, msg);
    }
}
