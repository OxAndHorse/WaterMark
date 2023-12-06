package cn.ac.iie.pkcgroup.dws.handler.app;

import cn.ac.iie.pkcgroup.dws.Constants;
import cn.ac.iie.pkcgroup.dws.comm.request.app.AppRegisterRequest;
import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
import cn.ac.iie.pkcgroup.dws.comm.response.app.entity.AppMetaData;
import cn.ac.iie.pkcgroup.dws.comm.response.app.entity.AppSimpleInfo;
import cn.ac.iie.pkcgroup.dws.data.dao.AppRegisterInfoRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.DemoUserRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.AppRegisterInfoEntity;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.DemoUserEntity;
import cn.ac.iie.pkcgroup.dws.handler.app.response.AppResponse;
import cn.ac.iie.pkcgroup.dws.handler.app.response.AppStatusCodes;
import cn.ac.iie.pkcgroup.dws.service.DatabaseService;
import cn.ac.iie.pkcgroup.dws.service.ReloadConfigService;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import cn.ac.iie.pkcgroup.dws.utils.crypto.HashUtils;
import cn.ac.iie.pkcgroup.dws.utils.crypto.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

import static cn.ac.iie.pkcgroup.dws.Constants.AUTH_KEY_LENGTH;

@DependsOn(value = "reloadConfigService")
@Component
@Slf4j
public class AppHandler {
    private AppRegisterInfoRepository appRegisterInfoRepository;
    private DemoUserRepository demoUserRepository;
    private ReloadConfigService reloadConfigService;
    private DatabaseService databaseService;

    final int SECRET_SEED_LENGTH = 32;
    @Value("${conf.property}")
    private String propertyPath;

    String propertyKeyTemplate = "%s.%s.%s";
    String jdbcUrlTemplate = "jdbc:%s://%s/%s?zeroDateTimeBehavior=convertToNull&characterEncoding=utf8&charset=utf8&parseTime=True&loc=Local";

    @Autowired
    public void setAppRegisterInfoRepository(AppRegisterInfoRepository repository) {
        appRegisterInfoRepository = repository;
    }

    @Autowired
    public void setDemoUserRepository(DemoUserRepository repository) {
        demoUserRepository = repository;
    }

    @Autowired
    public void setReloadConfigService(ReloadConfigService service) {
        reloadConfigService = service;
    }

    @Autowired
    public void setDatabaseService(DatabaseService service) {
        databaseService = service;
    }
    // TODO: ENCRYPT & DECRYPT password
    // Preserved
    private String decryptPassword(String encPwd) {
        return encPwd;
    }

    private String generateSystemIdByName(String systemName, String systemNickname) {
        String material = systemName + "_" + systemNickname;
        return UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String generateAuthKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[AUTH_KEY_LENGTH];
        random.nextBytes(bytes);
        return StringUtils.encodeToHex(bytes);
    }

    private String generateSecretSeed() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SEED_LENGTH];
        random.nextBytes(bytes);
        return StringUtils.encodeToHex(bytes);
    }

    private boolean validateAppRequest(AppRegisterRequest appRegisterRequest) {
        int functions = appRegisterRequest.getFunctions();
        if (functions > 0 && appRegisterRequest.getDbParams() == null) return false; // TODO: if used as an API server, dbParams can be ignored even if functions > 0, since the system can upload db csv file.
        return functions != 0 || appRegisterRequest.getDbParams() == null; // invalid request
    }

    public AppResponse saveAppRegisterInfoToDB(AppInfo appInfo) {
        AppRegisterRequest appRegisterRequest = appInfo.getAppRegisterRequest();
        String systemName = appRegisterRequest.getSystemName();
        String systemNickname = appRegisterRequest.getSystemNickname();
        String systemId = generateSystemIdByName(systemName, systemNickname);
        AppRegisterRequest.DBParams dbParams = appRegisterRequest.getDbParams();
        if (!validateAppRequest(appRegisterRequest)) {
            return new AppResponse(AppStatusCodes.CODE_INVALID_APP_REQUEST, AppStatusCodes.MSG_INVALID_APP_REQUEST);
        }
        String dbName = dbParams.getDbName();
        String dbIP = dbParams.getDbIP();
        String dbPort = dbParams.getDbPort();
        String dbType = dbParams.getDbType();
        appInfo.setSystemId(systemId);

        // check whether systemId exists.
        AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemId(systemId);
        String authKey;
        if (appRegisterInfo == null) {
            appRegisterInfo = new AppRegisterInfoEntity();
            authKey = generateAuthKey();
        } else {
            // check whether the record exists.
            AppRegisterInfoEntity appRegisterInfoEntity = appRegisterInfoRepository.findFirstBySystemIdAndDbNameAndDbIpAndDbPortAndDbType(systemId, dbName, dbIP, dbPort, dbType);
            if (appRegisterInfoEntity != null) {
                log.info("The system information has been recorded.");
                return new AppResponse(StatusCodes.CODE_SUCCESS, StatusCodes.MSG_SUCCESS);
            }
            authKey = appRegisterInfo.getSystemAuthKey();
        }
        appRegisterInfo.setSystemId(systemId);
        appRegisterInfo.setSystemName(systemName);
        appRegisterInfo.setSystemNickname(systemNickname);
        appRegisterInfo.setSystemAuthKey(authKey);
        appRegisterInfo.setDwUsage(appRegisterRequest.getDwUsage());
        appRegisterInfo.setFunctions(appRegisterRequest.getFunctions());
        appRegisterInfo.setDbName(dbName);
        appRegisterInfo.setDbName(dbName);
        appRegisterInfo.setDbIp(dbIP);
        appRegisterInfo.setDbPort(dbPort);
        appRegisterInfo.setDbType(dbType);
        appRegisterInfo.setSecretSeed(generateSecretSeed());
        appRegisterInfoRepository.saveAndFlush(appRegisterInfo);

        AppMetaData appMetaData = new AppMetaData();
        appMetaData.setSystemName(systemName);
        String systemPassword = PasswordUtils.generateRandomPassword(0, true); // TODO: Change useDefault to false when run as api server.
        appMetaData.setSystemPassword(systemPassword);
        appMetaData.setApiUser(systemId);
        appMetaData.setApiAuthKey(authKey);

        DemoUserEntity demoUser = demoUserRepository.findFirstById(systemName);
        if (demoUser == null) {
            demoUser = new DemoUserEntity();
            demoUser.setId(systemName);
            demoUser.setUid(HashUtils.doHashToHex(systemName.getBytes(StandardCharsets.UTF_8)));
            demoUser.setPassword(HashUtils.doHashToHex(systemPassword.getBytes(StandardCharsets.UTF_8)));
            demoUser.setRole(Constants.ROLE_OWNER);
            demoUser.setNickname(systemNickname);
            demoUser.setSystemId(systemId);
            demoUserRepository.saveAndFlush(demoUser);
        }

        return AppResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .appMetaData(appMetaData)
                .build();
    }

    public AppResponse saveAppRegisterInfoToConfig(AppInfo appInfo) {
        AppRegisterRequest appRegisterRequest = appInfo.getAppRegisterRequest();
        AppRegisterRequest.DBParams dbParams = appRegisterRequest.getDbParams();
        String dbName = dbParams.getDbName();
        String dbIp = dbParams.getDbIP();
        String dbPort = dbParams.getDbPort();
        String dbUrl = dbPort != null ? dbIp + ":" + dbPort : dbIp; // maybe use compose service name
        String dbUser = dbParams.getDbUser();
        String dbType = dbParams.getDbType().toLowerCase(Locale.ROOT);
        String dbPassword = decryptPassword(dbParams.getDbPassword());
        String systemId = appInfo.getSystemId();

        String jdbcUrl = String.format(jdbcUrlTemplate, dbType, dbUrl, dbName);
        String propertyJdbcUrlKey = String.format(propertyKeyTemplate, systemId, dbName, "jdbcUrl");
        String propertyUsernameKey = String.format(propertyKeyTemplate, systemId, dbName, "username");
        String propertyPasswordKey = String.format(propertyKeyTemplate, systemId, dbName, "password");

        try {
            FileReader fileReader = new FileReader(propertyPath);
            Properties properties = new Properties();
            properties.load(fileReader);
            fileReader.close();
            properties.setProperty(propertyJdbcUrlKey, jdbcUrl);
            properties.setProperty(propertyUsernameKey, dbUser);
            properties.setProperty(propertyPasswordKey, dbPassword);
            FileWriter fileWriter = new FileWriter(propertyPath);
            properties.store(fileWriter, appRegisterRequest.getSystemName());
            fileWriter.close();
            return new AppResponse(StatusCodes.CODE_SUCCESS, StatusCodes.MSG_SUCCESS);
        } catch (IOException e) {
            log.error("Read resource file fail.");
            return new AppResponse(AppStatusCodes.CODE_READ_PROPERTY_FAIL, AppStatusCodes.MSG_READ_PROPERTY_FAIL);
        }
    }

    public AppResponse checkDBConnection(AppInfo appInfo) {
        AppRegisterRequest appRegisterRequest = appInfo.getAppRegisterRequest();
        AppRegisterRequest.DBParams dbParams = appRegisterRequest.getDbParams();
        String dbName = dbParams.getDbName();
        String dbIp = dbParams.getDbIP();
        String dbPort = dbParams.getDbPort();
        String dbUrl = dbPort != null ? dbIp + ":" + dbPort : dbIp; // maybe use compose service name
        String dbType = dbParams.getDbType().toLowerCase(Locale.ROOT);

        String jdbcUrl = String.format(jdbcUrlTemplate, dbType, dbUrl, dbName);
        if (!databaseService.validateConnection(jdbcUrl, dbParams.getDbUser(), decryptPassword(dbParams.getDbPassword()))) {
            return new AppResponse(AppStatusCodes.CODE_INVALID_DB_PARAMS, AppStatusCodes.MSG_INVALID_DB_PARAMS);
        }
        return new AppResponse(StatusCodes.CODE_SUCCESS, StatusCodes.MSG_SUCCESS);
    }

    public AppResponse updateDBConnection(String systemId, AppInfo appInfo) {
        AppRegisterRequest appRegisterRequest = appInfo.getAppRegisterRequest();
        AppRegisterRequest.DBParams dbParams = appRegisterRequest.getDbParams();
        String dbName = dbParams.getDbName();
        String dbIp = dbParams.getDbIP();
        String dbPort = dbParams.getDbPort();
        String dbUrl = dbPort != null ? dbIp + ":" + dbPort : dbIp; // maybe use compose service name
        String dbType = dbParams.getDbType().toLowerCase(Locale.ROOT);

        String jdbcUrl = String.format(jdbcUrlTemplate, dbType, dbUrl, dbName);
        DatabaseService.DataSourceInfo dataSourceInfo = new DatabaseService.DataSourceInfo();
        dataSourceInfo.setJdbcUrl(jdbcUrl);
        dataSourceInfo.setUserName(dbParams.getDbUser());
        dataSourceInfo.setPassword(decryptPassword(dbParams.getDbPassword()));
        if (!reloadConfigService.reloadConfig(systemId, dbName, dataSourceInfo)) {
            return new AppResponse(AppStatusCodes.CODE_FAIL_TO_UPDATE_DB_CONFIG, AppStatusCodes.MSG_FAIL_TO_UPDATE_DB_CONFIG);
        }
        return new AppResponse(StatusCodes.CODE_SUCCESS, StatusCodes.MSG_SUCCESS);
    }

    public AppResponse getDBListBySystemId(String systemId) {
        List<AppRegisterInfoEntity> appRegisterInfoEntities = appRegisterInfoRepository.findAllBySystemId(systemId);
        if (appRegisterInfoEntities == null || appRegisterInfoEntities.size() == 0) {
            return new AppResponse(AppStatusCodes.CODE_UNREGISTERED_SYSTEM, AppStatusCodes.MSG_UNREGISTERED_SYSTEM);
        }
        ArrayList<String> dbList = new ArrayList<>(appRegisterInfoEntities.size());
        for (AppRegisterInfoEntity app:
             appRegisterInfoEntities) {
            if (dbList.contains(app.getDbName())) continue;
            dbList.add(app.getDbName());
        }
        return AppResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .dbList(dbList)
                .build();
    }

    public AppResponse getAppList() {
        List<AppRegisterInfoEntity> appRegisterInfoEntities = appRegisterInfoRepository.findAll();
        ArrayList<AppSimpleInfo> appSimpleInfos = new ArrayList<>(appRegisterInfoEntities.size());
        for (AppRegisterInfoEntity app:
                appRegisterInfoEntities) {
            AppSimpleInfo appSimpleInfo = new AppSimpleInfo();
            appSimpleInfo.setSystemId(app.getSystemId());
            appSimpleInfo.setSystemName(app.getSystemName());
            appSimpleInfo.setSystemNickname(app.getSystemNickname());
            appSimpleInfos.add(appSimpleInfo);
        }
        return AppResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .appSimpleInfos(appSimpleInfos)
                .build();
    }
}
