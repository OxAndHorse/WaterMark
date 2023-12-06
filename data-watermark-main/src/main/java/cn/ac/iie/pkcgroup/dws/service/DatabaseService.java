package cn.ac.iie.pkcgroup.dws.service;

import cn.ac.iie.pkcgroup.dws.comm.request.db.SelectedField;
import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
import cn.ac.iie.pkcgroup.dws.comm.response.db.entity.SimpleTableInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.ExtractInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.FieldModel;
import cn.ac.iie.pkcgroup.dws.core.db.model.RowExpansionInfo;
import cn.ac.iie.pkcgroup.dws.data.dao.AppRegisterInfoRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.TraceInfoRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.AppRegisterInfoEntity;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.TraceInfoEntity;
import cn.ac.iie.pkcgroup.dws.data.table.TableInfo;
import cn.ac.iie.pkcgroup.dws.data.table.TableInfoWithPK;
import cn.ac.iie.pkcgroup.dws.handler.app.response.AppStatusCodes;
import cn.ac.iie.pkcgroup.dws.service.response.DBBasicResponse;
import cn.ac.iie.pkcgroup.dws.service.response.DBBasicStatusCodes;
import cn.ac.iie.pkcgroup.dws.utils.SpringContextUtils;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.ac.iie.pkcgroup.dws.Constants.DEFAULT_TABLE_NAME;
import static cn.ac.iie.pkcgroup.dws.core.db.Constants.PRIMARY_KEY_DIVIDER;
import static cn.ac.iie.pkcgroup.dws.core.db.Parser.checkDataType;

@DependsOn(value = "pathInit")
@Service(value = "databaseService")
@Slf4j
public class DatabaseService {
    @Value("${conf.property}")
    private String propertyFilePath;

    private Map<String, DataSource> dataSourceMap;
    private AppRegisterInfoRepository appRegisterInfoRepository;
    private Map<String, ArrayList<ArrayList<String>>> dataCache;
    private TraceInfoRepository traceInfoRepository;

    @Autowired
    public void setAppRegisterInfoRepository(AppRegisterInfoRepository repository) {
        appRegisterInfoRepository = repository;
    }

    @Autowired
    public void setTraceInfoRepository(TraceInfoRepository repository) {
        traceInfoRepository = repository;
    }

    @Bean(value = "jdbcConnections")
    public DBConnectionMap initJDBCConnections() {
        Map<String, DataSource> connections = generateJDBCConnections();
        DBConnectionMap dbConnectionMap = new DBConnectionMap();
        if (connections == null) {
            dbConnectionMap.setDataSourceMap(new HashMap<>());
        } else {
            dbConnectionMap.setDataSourceMap(connections);
        }
        return dbConnectionMap;
    }

    public DataSource setDataSource(DataSourceInfo dataSourceInfo) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dataSourceInfo.getJdbcUrl());
        hikariConfig.setUsername(dataSourceInfo.getUserName());
        hikariConfig.setPassword(dataSourceInfo.getPassword());
        hikariConfig.addDataSourceProperty("connectionTimeout", "5000");
        hikariConfig.addDataSourceProperty("idleTimeout", "60000");
        hikariConfig.addDataSourceProperty("maximumPoolSize", "5");
        try {
            return new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            log.error("Cannot create Hikari pool for datasource: " + dataSourceInfo.getJdbcUrl());
            return null;
        }
    }

    private Map<String, DataSource> generateJDBCConnections() {
        Map<String, DataSourceInfo> dataSourceInfoMap = parsePropertiesFile(propertyFilePath);
        if (dataSourceInfoMap == null) return null;
        Map<String, DataSource> dataSourceMap = new HashMap<>(dataSourceInfoMap.size());
        for (String key :
                dataSourceInfoMap.keySet()) {
            DataSourceInfo dataSourceInfo = dataSourceInfoMap.get(key);
            DataSource ds = setDataSource(dataSourceInfo);
            if (ds != null)
                dataSourceMap.put(key, ds);
        }
        return dataSourceMap;
    }

    public boolean validateConnection(String jdbcUrl, String username, String password) {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            connection.close();
        } catch (SQLException e) {
            log.info("Invalid connection: " + jdbcUrl);
            return false;
        }
        return true;
    }

    public Map<String, DataSourceInfo> parsePropertiesFile(String fileName) {
        // 属性文件
        Properties props;
        try {
            FileReader fileReader = new FileReader(fileName);
            props = new Properties();
            props.load(fileReader);
        } catch (IOException e) {
            log.error("Read resource file fail.");
            return null;
        }

        Matcher matcher;
        Pattern pattern = Pattern.compile("^([0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12})\\.(\\w+)\\.(jdbcUrl|username|password)$");

        Map<String, DataSourceInfo> mapDataSource =
                new HashMap<>(4);
        // 根据配置文件解析数据源
        for (String keyProp : props.stringPropertyNames()) {
            matcher = pattern.matcher(keyProp);
            if (matcher.find()) {
                String systemId = matcher.group(1);
                String dbName = matcher.group(3);
                String dsPropName = matcher.group(4);
                String identicalKey = StringUtils.generateIdenticalKey(systemId, dbName);
                DataSourceInfo dataSourceInfo;

                if (mapDataSource.containsKey(identicalKey)) {
                    dataSourceInfo = mapDataSource.get(identicalKey);
                } else {
                    dataSourceInfo = new DataSourceInfo();
                }
                // TODO: password加密
                // 根据属性名给数据源属性赋值
                if ("jdbcUrl".equals(dsPropName)) {
                    dataSourceInfo.setJdbcUrl(props.getProperty(keyProp));
                } else if ("username".equals(dsPropName)) {
                    dataSourceInfo.setUserName(props.getProperty(keyProp));
                } else if ("password".equals(dsPropName)) {
                    dataSourceInfo.setPassword(props.getProperty(keyProp));
                }
                // key: systemId.dbName
                mapDataSource.put(identicalKey, dataSourceInfo);
            }
        }
        return mapDataSource;
    }

    @Data
    public static class DataSourceInfo {
        public String jdbcUrl;
        public String userName;
        public String password;

        public String toString() {
            return "(jdbcUrl:" + jdbcUrl + ", username:" + userName + ", password:" + password + ")";
        }
    }

    @Data
    public static class DBConnectionMap {
        Map<String, DataSource> dataSourceMap;
//        Map<String, Connection> connectionMap;
    }

    public DBBasicResponse getTableListByDBName(String systemId, String dbName) {
        DBConnectionMap dbConnectionMap = (DBConnectionMap) SpringContextUtils.getContext().getBean("jdbcConnections");
        dataSourceMap = dbConnectionMap.getDataSourceMap();

        // check record
        AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemIdAndDbName(systemId, dbName);
        if (appRegisterInfo == null)
            return DBBasicResponse.builder()
                    .statusCode(AppStatusCodes.CODE_UNREGISTERED_SYSTEM)
                    .message(AppStatusCodes.MSG_UNREGISTERED_SYSTEM)
                    .build();
        String identicalKey = StringUtils.generateIdenticalKey(systemId, dbName);
        Connection connection = null;
        try {
            DataSource ds = dataSourceMap.get(identicalKey);
            if (ds == null)
                return DBBasicResponse.builder()
                        .statusCode(DBBasicStatusCodes.CODE_NO_SOURCE_DB)
                        .message(DBBasicStatusCodes.MSG_NO_SOURCE_DB)
                        .build();
            connection = ds.getConnection();
            ArrayList<SimpleTableInfo> simpleTableInfoArrayList = new ArrayList<>();
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            String[] types = {"TABLE"};
            ResultSet tabs = databaseMetaData.getTables(null, null, "%", types);
            while (tabs.next()) {
                String tableName = tabs.getString("TABLE_NAME");
                String schema = tabs.getString("TABLE_SCHEM");

                ResultSet resultSet = databaseMetaData.getColumns(null, null, tableName, null);
                // Find primary keys
                ArrayList<String> pkNames = new ArrayList<>();
                ResultSet rs = databaseMetaData.getPrimaryKeys(tabs.getString("TABLE_CAT"), schema, tableName);
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    pkNames.add(name);
                }

                SimpleTableInfo simpleTableInfo = new SimpleTableInfo();
                simpleTableInfo.setTableName(tableName);
                ArrayList<SimpleTableInfo.TableField> tableFields = new ArrayList<>();
                // TODO: check initialized value
                while (resultSet.next()) {
                    SimpleTableInfo.TableField tableField = new SimpleTableInfo.TableField();
                    String colName = resultSet.getString("COLUMN_NAME");
                    if (pkNames.contains(colName)) {
                        tableField.setPrimaryKey(true);
                    }
                    tableField.setFieldName(colName);
                    int dataType = resultSet.getInt("DATA_TYPE");
                    String innerType = checkDataType(dataType);
                    tableField.setFieldType(innerType);
                    tableFields.add(tableField);
                }
                simpleTableInfo.setTableFields(tableFields);
                simpleTableInfo.setTableFieldCount(tableFields.size());
                simpleTableInfo.setDefault(tableName.equals(DEFAULT_TABLE_NAME));
                simpleTableInfoArrayList.add(simpleTableInfo);
            }
            connection.close();
            return DBBasicResponse.builder()
                    .statusCode(StatusCodes.CODE_SUCCESS)
                    .message(StatusCodes.MSG_SUCCESS)
                    .simpleTableInfos(simpleTableInfoArrayList)
                    .build();
        } catch (SQLException e) {
            log.error("Execute SQL error: ", e);
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    log.error("Unable to close connection:", ex);
                }
            }
            return DBBasicResponse.builder()
                    .statusCode(DBBasicStatusCodes.CODE_SQL_ERROR)
                    .message(DBBasicStatusCodes.MSG_SQL_ERROR)
                    .build();
        }
    }

    public DBBasicResponse getDataByDBNameAndTableName(String systemId, String dbName, String tableName, int page, int pageCount) {
        DBConnectionMap dbConnectionMap = (DBConnectionMap) SpringContextUtils.getContext().getBean("jdbcConnections");
        dataSourceMap = dbConnectionMap.getDataSourceMap();

        // check record
        AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemIdAndDbName(systemId, dbName);
        if (appRegisterInfo == null)
            return DBBasicResponse.builder()
                    .statusCode(AppStatusCodes.CODE_UNREGISTERED_SYSTEM)
                    .message(AppStatusCodes.MSG_UNREGISTERED_SYSTEM)
                    .build();

        ArrayList<ArrayList<String>> dataSet = new ArrayList<>();
        ArrayList<ArrayList<String>> pagedDataSet = new ArrayList<>();
        Connection connection = null;
        try {
            String identicalKey = StringUtils.generateIdenticalKey(systemId, dbName);
            DataSource ds = dataSourceMap.get(identicalKey);
            if (ds == null)
                return DBBasicResponse.builder()
                        .statusCode(DBBasicStatusCodes.CODE_NO_SOURCE_DB)
                        .message(DBBasicStatusCodes.MSG_NO_SOURCE_DB)
                        .build();
            connection = ds.getConnection();
            String cacheKey = StringUtils.generateIdenticalKey(systemId, dbName, tableName);
            int totalCount = 0;
            int colCount;
            if (dataCache == null) dataCache = new HashMap<>();
            if (dataCache.containsKey(cacheKey)) {
                dataSet = dataCache.get(cacheKey);
                totalCount = dataSet.size();
                colCount = dataSet.get(0).size();
            } else {
                String querySql = String.format("SELECT * FROM `%s`", tableName);
                PreparedStatement preparedStatement = connection.prepareStatement(querySql);
                ResultSet rs = preparedStatement.executeQuery();
                colCount = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    ArrayList<String> tmpList = new ArrayList<>(colCount);
                    for (int i = 0; i < colCount; i++) {
                        String data = rs.getString(i + 1);
                        tmpList.add(data);
                    }
                    dataSet.add(tmpList);
                    totalCount++;
                }
                dataCache.put(cacheKey, dataSet);
                preparedStatement.close();
                rs.close();
            }
            int counter = 0;
            int startPos = (page - 1) * pageCount;
            if (totalCount > startPos) {
                while (counter < pageCount) {
                    ArrayList<String> tmpList = new ArrayList<>(colCount);
                    for (int i = 0; i < colCount; i++) {
                        String data = dataSet.get(startPos + counter).get(i);
                        tmpList.add(data);
                    }
                    pagedDataSet.add(tmpList);
                    counter++;
                }
            }
            connection.close();
            return DBBasicResponse.builder()
                    .statusCode(StatusCodes.CODE_SUCCESS)
                    .message(StatusCodes.MSG_SUCCESS)
                    .pagedDataSet(pagedDataSet)
                    .totalCount(totalCount)
                    .build();
        } catch (SQLException e) {
            log.error("Execute SQL error: ", e);
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    log.error("Unable to close connection:", ex);
                }
            }
            return DBBasicResponse.builder()
                    .statusCode(DBBasicStatusCodes.CODE_SQL_ERROR)
                    .message(DBBasicStatusCodes.MSG_SQL_ERROR)
                    .build();
        }
    }

    public String getSystemKeyCodeById(String systemId) {
        AppRegisterInfoEntity appRegisterInfo = appRegisterInfoRepository.findFirstBySystemId(systemId);
        if (appRegisterInfo == null) return null;
        return appRegisterInfo.getSystemAuthKey();
    }

    public boolean recordTraceInfo(BasicTraceInfo basicTraceInfo, ArrayList<SelectedField> selectedFields) {
        TraceInfoEntity traceInfoEntity = new TraceInfoEntity();
        traceInfoEntity.setSystemId(basicTraceInfo.getSystemId());
        traceInfoEntity.setDbName(basicTraceInfo.getDbName());
        traceInfoEntity.setTableName(basicTraceInfo.getTableName());
        traceInfoEntity.setWatermark(basicTraceInfo.getWatermark().toString());
        traceInfoEntity.setEmbeddedMsg(basicTraceInfo.getEmbeddingMessage());
        traceInfoEntity.setAllowColumnExpansion((byte) (basicTraceInfo.isAllowColumnExpansion() ? 1 : 0));
        traceInfoEntity.setAllowRowExpansion((byte) (basicTraceInfo.isAllowRowExpansion() ? 1 : 0));

        Gson gson = new Gson();
        if (basicTraceInfo.isUseColumn()) {
            String wmFields = gson.toJson(selectedFields);
            if (wmFields == null) return false;
            traceInfoEntity.setWmFields(wmFields);
        }
        // row expansion
        if (basicTraceInfo.isAllowRowExpansion()) {
            String rowExpansionInfo = gson.toJson(basicTraceInfo.getRowExpansionInfos());
            if (rowExpansionInfo == null) return false;
            traceInfoEntity.setRowExpansionAlgorithm(rowExpansionInfo);
        }
        // column expansion
        if (basicTraceInfo.isAllowColumnExpansion()) {
            String colExpansionInfo = gson.toJson(basicTraceInfo.getColumnExpansionInfos());
            if (colExpansionInfo == null) return false;
            traceInfoEntity.setColumnExpansionAlgorithm(colExpansionInfo);
        }

        // set pk, do not set when only row expansion is set
        if (basicTraceInfo.isUseColumn() || basicTraceInfo.isAllowColumnExpansion()) {
            StringBuilder sb = new StringBuilder();
            for (FieldModel.FieldUnit pk :
                    basicTraceInfo.getFieldModel().getPrimaryKeys()) {
                sb.append(pk.getFieldName()).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            traceInfoEntity.setPrimaryKeys(sb.toString());
        }

        Timestamp ts = new Timestamp(new Date().getTime());
        traceInfoEntity.setRecordTime(ts);
        traceInfoRepository.saveAndFlush(traceInfoEntity);
        return true;
    }

    public ArrayList<TraceInfoEntity> getTraceInfo(ExtractInfo extractInfo) {
        List<TraceInfoEntity> traceInfoEntities;
        traceInfoEntities = traceInfoRepository.findAllBySystemIdAndDbNameAndTableName(extractInfo.getSystemId(),
                extractInfo.getDbName(), extractInfo.getTableName());
        if (traceInfoEntities == null) return null;
        return (ArrayList<TraceInfoEntity>) traceInfoEntities;
    }

    public TableInfoWithPK getTableInfoWithPK(String tableName, DatabaseMetaData databaseMetaData, ResultSet tabs) throws SQLException {
        TableInfoWithPK tableInfoWithPK;
        String schema = tabs.getString("TABLE_SCHEM");

        ResultSet resultSet = databaseMetaData.getColumns(null, schema, tableName, null);
        int colCount = 0;
        if (resultSet.last()) {
            colCount = resultSet.getRow();
            resultSet.beforeFirst();
        }
        ArrayList<String> cols = new ArrayList<>(colCount);
        Map<String, FieldModel.FieldUnit> columnsInfo = new HashMap<>();
        // Find primary keys
        StringBuilder pk = new StringBuilder();
        ArrayList<String> pkName = new ArrayList<>();
        ArrayList<Integer> pkIndex = new ArrayList<>();
        ResultSet rs = databaseMetaData.getPrimaryKeys(tabs.getString("TABLE_CAT"), schema, tableName);
        while (rs.next()) {
            String name = rs.getString("COLUMN_NAME");
            pk.append(name).append(PRIMARY_KEY_DIVIDER);
            pkName.add(name);
        }
        pk.deleteCharAt(pk.length() - 1);

        // TODO: check initialized value
        int colIndex = 0;
        while (resultSet.next()) {
            FieldModel.FieldUnit fieldUnit = new FieldModel.FieldUnit();
            String colName = resultSet.getString("COLUMN_NAME");
            if (pkName.contains(colName)) {
                pkIndex.add(colIndex);
                fieldUnit.setPrimaryKey(true);
            }
            int dataType = resultSet.getInt("DATA_TYPE");
            int colSize = resultSet.getInt("COLUMN_SIZE");
            String innerType = checkDataType(dataType);
            cols.add(colName);
            fieldUnit.setFieldName(colName);
            fieldUnit.setFieldIndex(colIndex++);
            fieldUnit.setFieldType(innerType);
            fieldUnit.setFieldSize(colSize);
            columnsInfo.put(colName, fieldUnit);
        }

        TableInfo tableInfo = new TableInfo();
        tableInfo.setColumnName(cols);
        tableInfo.setColumnsInfo(columnsInfo);
        tableInfoWithPK = new TableInfoWithPK();
        tableInfoWithPK.setTableInfo(tableInfo);
        tableInfoWithPK.setPrimaryKey(pk.toString());
        tableInfoWithPK.setPrimaryKeyList(pkName);
        tableInfoWithPK.setPkIndex(pkIndex);
        return tableInfoWithPK;
    }
}
