package cn.ac.iie.pkcgroup.dws.handler.app;

import cn.ac.iie.pkcgroup.dws.comm.request.app.AppRegisterRequest;
import lombok.Data;

@Data
public class AppInfo {
    AppRegisterRequest appRegisterRequest;
    String systemId;
}
