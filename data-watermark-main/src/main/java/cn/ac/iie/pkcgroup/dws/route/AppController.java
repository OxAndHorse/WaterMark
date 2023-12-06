package cn.ac.iie.pkcgroup.dws.route;

import cn.ac.iie.pkcgroup.dws.comm.request.app.AppRegisterRequest;
import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
import cn.ac.iie.pkcgroup.dws.comm.response.app.AppListResponse;
import cn.ac.iie.pkcgroup.dws.comm.response.app.DBListResponse;
import cn.ac.iie.pkcgroup.dws.comm.response.app.RegisterResponse;
import cn.ac.iie.pkcgroup.dws.comm.response.app.entity.AppMetaData;
import cn.ac.iie.pkcgroup.dws.handler.app.AppInfo;
import cn.ac.iie.pkcgroup.dws.comm.response.app.entity.DBMetaData;
import cn.ac.iie.pkcgroup.dws.handler.app.response.AppResponse;
import cn.ac.iie.pkcgroup.dws.handler.app.AppHandler;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import cn.ac.iie.pkcgroup.dws.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.Constants.DEFAULT_DB_NAME;
import static cn.ac.iie.pkcgroup.dws.Constants.NORMAL_CODE;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/dws/app")
public class AppController {
    private AppHandler appHandler;
    private TokenService tokenService;
    private String appAlreadyExist = "当前应用已注册，请勿重复注册！";

    @Autowired
    public void setAppHandler(AppHandler handler) {
        appHandler = handler;
    }

    @Autowired
    public void setTokenService(TokenService service) {
        tokenService = service;
    }

    private boolean isNormal(int code) {
        return code == NORMAL_CODE;
    }

    /**
     * admin only
     */
    @PostMapping(value = "/register")
    public BasicResponse register(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestBody @Valid AppRegisterRequest appRegisterRequest
    ) {
        if (!tokenService.isAdmin(token)) return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        AppInfo appInfo = new AppInfo();
        appInfo.setAppRegisterRequest(appRegisterRequest);
        // 1. Check database connection
        AppResponse appResponse = appHandler.checkDBConnection(appInfo);
        if (!isNormal(appResponse.getStatusCode())) return appResponse;
        // 2. Record to table app_register_info
        appResponse = appHandler.saveAppRegisterInfoToDB(appInfo);
        if (!isNormal(appResponse.getStatusCode())) return appResponse;
        AppMetaData appMetaData = appResponse.getAppMetaData();
        RegisterResponse registerResponse;
        // The application has registered.
        if (appMetaData != null) {
        registerResponse = RegisterResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .apiUser(appMetaData.getApiUser())
                .apiAuthKey(appMetaData.getApiAuthKey())
                .systemPassword(appMetaData.getSystemPassword())
                .build();
            // 3. Record to file persistence.properties
            appResponse = appHandler.saveAppRegisterInfoToConfig(appInfo);
            if (!isNormal(appResponse.getStatusCode())) return appResponse;
            // 4. Update database connection
            appResponse = appHandler.updateDBConnection(appMetaData.getApiUser(), appInfo);
            if (!isNormal(appResponse.getStatusCode())) return appResponse;
        } else {
            registerResponse = RegisterResponse.builder()
                    .statusCode(StatusCodes.CODE_SUCCESS)
                    .message(appAlreadyExist)
                    .build();
        }

        return registerResponse;
    }

    /**
     * admin only
     */
    @GetMapping(value = "/list")
    public BasicResponse getAppList(
            @RequestHeader("X-USER-TOKEN") String token
    ) {
        if (!tokenService.isAdmin(token)) return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        AppResponse appResponse = appHandler.getAppList();
        if (!isNormal(appResponse.getStatusCode())) return appResponse;
        return AppListResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .systemCount(appResponse.getAppSimpleInfos().size())
                .systemList(appResponse.getAppSimpleInfos())
                .build();
    }

    @GetMapping(value = "/dbList")
    public BasicResponse getDBList(
            @RequestHeader("X-USER-TOKEN") String token
    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null) return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        AppResponse appResponse = appHandler.getDBListBySystemId(systemId);
        if (!isNormal(appResponse.getStatusCode())) return appResponse;
        DBListResponse dbListResponse = DBListResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .build();
        ArrayList<DBMetaData> dbInfoList = new ArrayList<>(appResponse.getDbList().size());
        for (String dbName:
             appResponse.getDbList()) {
            DBMetaData dbMetaData = new DBMetaData();
            dbMetaData.setDbName(dbName);
            dbMetaData.setDefault(dbName.equals(DEFAULT_DB_NAME));
            dbInfoList.add(dbMetaData);
        }
        dbListResponse.setDbInfoList(dbInfoList);
        dbListResponse.setDbCount(dbInfoList.size());

        return dbListResponse;
    }


}
