package cn.ac.iie.pkcgroup.dws.service;

import cn.ac.iie.pkcgroup.dws.utils.SpringContextUtils;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service(value = "reloadConfigService")
@DependsOn(value = "databaseService")
@Slf4j
public class ReloadConfigService {
    private static final String HASH_ALG = "SHA-256";
    private String serialNumber;
    private DatabaseService databaseService;

    @Value("${conf.property}")
    private String propertyPath;

    @Autowired
    public void setDatabaseService(DatabaseService service) {
        databaseService = service;
        serialNumber = computeHash();
    }

    private String hexEncoding(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            final String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String computeHash() {
        try {
            File propertyFile = new File(propertyPath);
            InputStream inputStream = Files.newInputStream(propertyFile.toPath());
            byte[] input = new byte[inputStream.available()];
            if (inputStream.read(input) < 0) {
                return null;
            }
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALG);
            byte[] hash = messageDigest.digest(input);
            return hexEncoding(hash);
        } catch (IOException e) {
            log.error("Cannot read config file when init reload-config-service!");
            return null;
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot find Hash algorithm: " + HASH_ALG);
            return null;
        }
    }

    private boolean validateChanges() {
        if (serialNumber == null) return false;
        String latestSerialNumber = computeHash();
        if (latestSerialNumber == null) return false;
        return !serialNumber.equals(latestSerialNumber);
    }

    @Deprecated
    public boolean reloadConfig() {
        if (validateChanges()) {
            // TODO: change bean
            DatabaseService.DBConnectionMap dbConnectionMap = (DatabaseService.DBConnectionMap) SpringContextUtils.getContext().getBean("jdbcConnections");
            Map<String, DataSource> connectionMap = dbConnectionMap.getDataSourceMap();
            Map<String, DatabaseService.DataSourceInfo> dataSourceInfoMap = databaseService.parsePropertiesFile(propertyPath);
            assert dataSourceInfoMap != null;
            for (String mapKey :
                    dataSourceInfoMap.keySet()) {
                DatabaseService.DataSourceInfo dataSourceInfo = dataSourceInfoMap.get(mapKey);
                if (!connectionMap.containsKey(mapKey)) {
//                    try {
//                        Connection connection = DriverManager.getConnection(dataSourceInfo.jdbcUrl, dataSourceInfo.userName, dataSourceInfo.password);
                    DataSource ds = databaseService.setDataSource(dataSourceInfo);
                    connectionMap.put(mapKey, ds);
//                    } catch (SQLException e) {
//                        log.error("JDBC generate connections fail: " + dataSourceInfo.jdbcUrl);
//                        return false;
//                    }
                }
            }
            serialNumber = computeHash();
        } else {
            log.info("The config is latest.");
        }
        return true;
    }

    public boolean reloadConfig(String systemId, String dbName, DatabaseService.DataSourceInfo dataSourceInfo) {
        DatabaseService.DBConnectionMap dbConnectionMap = (DatabaseService.DBConnectionMap) SpringContextUtils.getContext().getBean("jdbcConnections");
        Map<String, DataSource> dataSourceMap = dbConnectionMap.getDataSourceMap();
        String mapKey = StringUtils.generateIdenticalKey(systemId, dbName);
        if (!dataSourceMap.containsKey(mapKey)) {
            log.info("Reload database table mapping.");
            DataSource ds = databaseService.setDataSource(dataSourceInfo);
            dataSourceMap.put(mapKey, ds);
        }
        return true;
    }
}
