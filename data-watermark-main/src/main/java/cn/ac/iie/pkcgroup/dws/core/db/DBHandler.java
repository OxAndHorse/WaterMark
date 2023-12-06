package cn.ac.iie.pkcgroup.dws.core.db;

import cn.ac.iie.pkcgroup.dws.comm.request.db.SelectedField;
import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
import cn.ac.iie.pkcgroup.dws.config.model.ConfigUnit;
import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.*;
import cn.ac.iie.pkcgroup.dws.core.db.model.*;
import cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator;
import cn.ac.iie.pkcgroup.dws.core.db.response.DBResponse;
import cn.ac.iie.pkcgroup.dws.core.db.response.DBStatusCodes;
import cn.ac.iie.pkcgroup.dws.core.db.utils.CSVUtils;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.TraceInfoEntity;
import cn.ac.iie.pkcgroup.dws.data.table.TableInfoWithPK;
import cn.ac.iie.pkcgroup.dws.data.table.TableMap;
import cn.ac.iie.pkcgroup.dws.service.DatabaseService;
import cn.ac.iie.pkcgroup.dws.utils.SpringContextUtils;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import cn.ac.iie.pkcgroup.dws.utils.crypto.HashUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.ac.iie.pkcgroup.dws.core.db.Constants.*;
import static cn.ac.iie.pkcgroup.dws.core.db.Parser.*;
import static cn.ac.iie.pkcgroup.dws.utils.StringUtils.generateIdenticalKey;
import static cn.ac.iie.pkcgroup.dws.utils.StringUtils.getRowPrimaryKeyValue;

@Component
@DependsOn({"tableMap"})
@Data
@Slf4j
public class DBHandler {
    private TableMap tableMap;
    private Map<String, DataSource> connections;
    @Value("${conf.tmpRoot}")
    private String tmpRoot;
    private DB csvMapping;
    private DB selectedTableCache;
    private DatabaseService databaseService;
    private RowExpansionConf rowExpansionConf;

    private final String DW_CSV_SUFFIX = "+wm";
    private final String CSV_CONN = "+";
    @Value("${conf.dwThreshold}")
    private double threshold; // 相似度低于0.7认为水印被破坏

    @Autowired
    public void setDatabaseService(DatabaseService service) {
        databaseService = service;
    }

    @Autowired
    public void setCSVMapping(@Qualifier("csvFileMapping") DB db) {
        csvMapping = db;
    }

    @Autowired
    public void setSelectedTableMapping(@Qualifier("selectedTableCache") DB db) {
        selectedTableCache = db;
    }

    @Autowired
    public void setTableMap(@Qualifier("tableMap") TableMap map) {
        tableMap = map;
    }

    @Autowired
    public void setConnections(DatabaseService.DBConnectionMap connectionMap) {
        // TODO: close all connections
        // All connections are established by the scheduled task via loading database config files
        // Maybe they should be established only when requests coming
        connections = connectionMap.getDataSourceMap();
    }

    @Autowired
    public void setRowExpansionConf(RowExpansionConf conf) {
        rowExpansionConf = conf;
    }

    /**
     * Fetch data from the specified db table and all fields are saved as String.
     * Also get the table information, such as primary key, field type.
     */
    private DatasetWithPK fetchDatasetByTableName(String systemId, String dbName, String tableName, TableMap tableMap, String querySql) {
        String connectionKey = generateIdenticalKey(systemId, dbName);
        String mapKey = generateIdenticalKey(systemId, dbName, tableName);
        DatasetWithPK datasetWithPK = new DatasetWithPK();
        ArrayList<ArrayList<String>> dataSet = new ArrayList<>();
        ArrayList<Integer> pkIndex = new ArrayList<>();
        if (querySql == null) {
            // use default
            querySql = String.format("SELECT * FROM `%s`", tableName);
        }
        PreparedStatement preparedStatement;
        Connection conn = null;
        try {
            DataSource ds = connections.get(connectionKey);
            if (ds == null) {
                log.info("No connection is ready for the table {}.", mapKey);
                return null;
            }
            conn = ds.getConnection();
            preparedStatement = conn.prepareStatement(querySql);
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsMetadata = rs.getMetaData();
            int colCount = rsMetadata.getColumnCount();
            // record selected fields from sql
            ArrayList<FieldModel.FieldUnit> metaData = new ArrayList<>(colCount);
            ArrayList<String> pkList = tableMap.getInfoMap().get(mapKey).getPrimaryKeyList();
            for (int i = 1; i <= colCount; ++i) {
                FieldModel.FieldUnit fieldUnit = new FieldModel.FieldUnit();
                fieldUnit.setFieldName(rsMetadata.getColumnName(i));
                fieldUnit.setFieldTypeCode(rsMetadata.getColumnType(i));
                fieldUnit.setFieldSize(rsMetadata.getColumnDisplaySize(i));
                fieldUnit.setAutoIncrement(rsMetadata.isAutoIncrement(i));
                for (String pkName : pkList) {
                    if (pkName.equals(fieldUnit.getFieldName())) {
                        fieldUnit.setPrimaryKey(true);
                        pkIndex.add(i - 1);
                        break;
                    }
                }
                metaData.add(fieldUnit);
            }
            datasetWithPK.setMetaData(metaData);
            while (rs.next()) {
                ArrayList<String> tmpList = new ArrayList<>(colCount);
                for (int i = 0; i < colCount; i++) {
                    String data = rs.getString(i + 1);
                    tmpList.add(data);
                }
                dataSet.add(tmpList);
            }
            preparedStatement.close();
            rs.close();
            conn.close();
        } catch (SQLException e) {
            log.error("Fail to fetch data from the table since: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Unable to close connection:", ex);
                }
            }
            return null;
        }
        Map<String, ArrayList<String>> dsMap = generateDataSetWithPK(dataSet, pkIndex);
        datasetWithPK.setDataset(dsMap);
        return datasetWithPK;
    }

    private String generateSuffix() {
        return DW_TABLE_SUFFIX;
    }

    private String generateNewTableName(String sourceTable) {
        return sourceTable + generateSuffix();
    }

    private String parseSourceTable(String tableName) {
        String suffix = generateSuffix();
        if (tableName.contains(suffix)) {
            int end = tableName.indexOf(suffix);
            return tableName.substring(0, end);
        } else {
            return null;
        }
    }

    private String generateCSVFilePath(String systemId, String dbName, String tableName) {
        return tmpRoot + "/" + systemId + CSV_CONN + dbName + CSV_CONN + tableName + DW_CSV_SUFFIX + ".csv";
    }

    private String generateDataID(String systemId, String dbName, String tableName) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String timestamp = df.format(new Date());
        byte[] hash = HashUtils.doHash(timestamp + systemId);
        if (hash == null) return null;
        String suffix = StringUtils.encodeToHex(hash);
        return dbName + "_" + tableName + "_" + suffix;
    }

    private void cacheCSVFileMapping(String systemId, String dataId, String filePath) {
        String value = systemId + CSV_MAPPING_DIVIDER + filePath;
        csvMapping.put(dataId.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
    }

    public DBResponse getCSVFile(String systemId, String dataId) {
        byte[] csvFilePath = csvMapping.get(dataId.getBytes(StandardCharsets.UTF_8));
        if (csvFilePath == null)
            return new DBResponse(DBStatusCodes.CODE_NO_SUCH_CSV_DATA, DBStatusCodes.MSG_NO_SUCH_CSV_DATA);
        String value = new String(csvFilePath, StandardCharsets.UTF_8);
        int divide = value.indexOf(CSV_MAPPING_DIVIDER);
        if (divide <= 0) return new DBResponse(DBStatusCodes.CODE_INTERNAL_ERROR, DBStatusCodes.MSG_INTERNAL_ERROR);
        String recordSystemId = value.substring(0, divide);
        if (!recordSystemId.equals(systemId))
            return new DBResponse(DBStatusCodes.CODE_INTERNAL_ERROR, DBStatusCodes.MSG_INTERNAL_ERROR);
        String path = value.substring(divide + 1);
        String[] list = path.split("/");
        String fileName = list[list.length - 1];
        try {
            byte[] csvFile = Files.readAllBytes(Paths.get(path));
            return DBResponse.builder()
                    .statusCode(DBStatusCodes.CODE_SUCCESS)
                    .message(DBStatusCodes.MSG_SUCCESS)
                    .csvFile(csvFile)
                    .csvFileName(fileName)
                    .build();
        } catch (IOException e) {
            log.error("Get CSV error. ", e);
            return new DBResponse(DBStatusCodes.CODE_READ_CSV_DATA_FAIL, DBStatusCodes.MSG_READ_CSV_DATA_FAIL);
        }
    }


    private boolean exportNewDataToDB(DatasetWithPK datasetWithPK, String systemId, String dbName, String oriTableName, int colCount, String outputTable) {
        AtomicBoolean isSuccess = new AtomicBoolean(false);
        String newTableName = outputTable == null ? generateNewTableName(oriTableName) : outputTable;
        String dropTableSql = String.format("DROP TABLE IF EXISTS `%s`", newTableName);
        String createNewTableSql = String.format("CREATE TABLE IF NOT EXISTS `%s` LIKE `%s`", newTableName, oriTableName);
        String[] placeholders = new String[colCount];
        Arrays.fill(placeholders, "?");
        String insertSql = String.format("INSERT INTO `%s` VALUES (%s)", newTableName, String.join(",", placeholders));
        String connectionKey = StringUtils.generateIdenticalKey(systemId, dbName);
        PreparedStatement preparedStatement;
        Connection conn = null;
        try {
            DataSource ds = connections.get(connectionKey);
            if (ds == null) {
                log.info("No connection is ready.");
                return false;
            }
            conn = ds.getConnection();
            preparedStatement = conn.prepareStatement(dropTableSql);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            preparedStatement = conn.prepareStatement(createNewTableSql);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            PreparedStatement insertStatement = conn.prepareStatement(insertSql);
            Map<String, ArrayList<String>> dataSet = datasetWithPK.getDataset();
            for (String k :
                    dataSet.keySet()) {
                try {
                    ArrayList<String> v = dataSet.get(k);
                    for (int i = 0; i < v.size(); ++i) {
                        insertStatement.setString(i + 1, v.get(i));
                    }
                    insertStatement.addBatch();
                } catch (SQLException e) {
                    log.error("Fail to set INSERT statement.");
                    isSuccess.set(false);
                    return isSuccess.get();
                }
            }
            insertStatement.executeBatch();
            insertStatement.close();
            conn.close();
            isSuccess.set(true);
        } catch (SQLException e) {
            log.error("Fail to create new table.");
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Unable to close connection:", ex);
                }
            }
            isSuccess.set(false);
        }
        return isSuccess.get();
    }

    private boolean exportNewDataToLocalCSV(String fileName, ArrayList<String> colName, ArrayList<ArrayList<String>> dataCollection, boolean isAppend) {
        CSVUtils csvUtils = new CSVUtils(fileName);
        String[] headers = null;
        if (!isAppend) {
            headers = new String[colName.size()];
            for (int i = 0; i < colName.size(); ++i) {
                headers[i] = colName.get(i);
            }
        }

        return csvUtils.exportCSV(dataCollection, headers, isAppend);
    }

    private CSVUtils.CSVInfo readDataFromCSV(MultipartFile csvFile) {
        CSVUtils csvUtils = new CSVUtils();
        return csvUtils.parseCSV(csvFile);
    }

    private Map<String, ArrayList<String>> generateDataSetWithPK(ArrayList<ArrayList<String>> rawData, ArrayList<Integer> pkIndexList) {
        if (pkIndexList.isEmpty()) return null;
        Map<String, ArrayList<String>> dsMap = new HashMap<>(rawData.size());
        for (ArrayList<String> rowData :
                rawData) {
            String rowPK = getRowPrimaryKeyValue(pkIndexList, rowData);
            dsMap.put(rowPK, rowData);
        }
        return dsMap;
    }


    private boolean updateTableMap(String systemId, String dbName, String tableName) {
        String mapKey = StringUtils.generateIdenticalKey(systemId, dbName, tableName);
        String connectionKey = StringUtils.generateIdenticalKey(systemId, dbName);
        Map<String, TableInfoWithPK> map = tableMap.getInfoMap();

        DataSource ds = connections.get(connectionKey);
        if (ds == null) {
            DatabaseService.DBConnectionMap dbConnectionMap = (DatabaseService.DBConnectionMap) SpringContextUtils.getContext().getBean("jdbcConnections");
            connections = dbConnectionMap.getDataSourceMap();
            ds = connections.get(connectionKey);
            if (ds == null) {
                log.error("No such database connection: " + connectionKey);
                return false;
            }
        }
        Connection connection = null;
        try {
            connection = ds.getConnection();
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet tabs = databaseMetaData.getTables(null, null, tableName, null);
            while (tabs.next()) {
                TableInfoWithPK tableInfoWithPK = databaseService.getTableInfoWithPK(tableName, databaseMetaData, tabs);
                map.put(mapKey, tableInfoWithPK);
            }
            // cache embed table
            ConfigUnit conf = new ConfigUnit();
            conf.setSystemId(systemId);
            conf.setTableName(tableName);
            conf.setDbName(dbName);
            String target = new Gson().toJson(conf);
            for (Map.Entry<byte[], byte[]> kv : selectedTableCache) {
                byte[] tableConfigsBytes = kv.getKey();
                if (tableConfigsBytes.length > 0) {
                    String strJson = new String(tableConfigsBytes, StandardCharsets.UTF_8);
                    if (strJson.equals(target)) {
                        return true;
                    }
                }
            }
            selectedTableCache.put(target.getBytes(StandardCharsets.UTF_8), tableName.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (SQLException e) {
            log.error("Update table map error. ", e);
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    log.error("Unable to close connection:", ex);
                }
            }
            return false;
        }
    }

    public DBResponse getEmbeddedTableList(String targetSystemId) {
        ArrayList<EmbeddedTableInfo> embeddedTableInfos = new ArrayList<>();
        Map<String, ArrayList<String>> map = new HashMap<>();
        for (Map.Entry<byte[], byte[]> kv : selectedTableCache) {
            byte[] tableConfigsBytes = kv.getKey();
            if (tableConfigsBytes.length > 0) {
                String strJson = new String(tableConfigsBytes, StandardCharsets.UTF_8);
                ConfigUnit conf = new Gson().fromJson(strJson, ConfigUnit.class);
                if (conf != null && conf.getSystemId().equals(targetSystemId)) {
                    ArrayList<String> tableNames;
                    String dbName = conf.getDbName();
                    if (map.containsKey(dbName)) {
                        tableNames = map.get(dbName);
                    } else {
                        tableNames = new ArrayList<>();
                    }
                    tableNames.add(conf.getTableName());
                    map.put(dbName, tableNames);
                }
            }
        }
        for (String dbName :
                map.keySet()) {
            EmbeddedTableInfo embeddedTableInfo = new EmbeddedTableInfo();
            embeddedTableInfo.setDbName(dbName);
            embeddedTableInfo.setTables(map.get(dbName));
            embeddedTableInfos.add(embeddedTableInfo);
        }
        return DBResponse.builder()
                .statusCode(DBStatusCodes.CODE_SUCCESS)
                .message(DBStatusCodes.MSG_SUCCESS)
                .embeddedTableList(embeddedTableInfos)
                .build();
    }

    // TODO: clarify how to choose the embedding fields.
    private ArrayList<SelectedField> chooseEmbeddingFields(TableInfoWithPK tableInfoWithPK) {
        ArrayList<SelectedField> selectedFields;
        Map<String, FieldModel.FieldUnit> columnsInfo = tableInfoWithPK.getTableInfo().getColumnsInfo();
        if (columnsInfo == null) return null;
        selectedFields = new ArrayList<>();
        for (FieldModel.FieldUnit fieldUnit : columnsInfo.values()) {
            SelectedField field = new SelectedField();
            String algorithm = chooseAlgorithm(fieldUnit);
            if (algorithm != null) {
                field.setFieldName(fieldUnit.getFieldName());
                field.setAlgorithm(algorithm);
            }
        }
        return selectedFields;
    }

    /*
     TODO: rollback when fail
     */
    public DBResponse embed(EmbeddingInfo embeddingInfo) {
        String dbName = embeddingInfo.getDbName();
        String tableName = embeddingInfo.getTableName();
        String systemId = embeddingInfo.getSystemId();
        String identicalKey = StringUtils.generateIdenticalKey(systemId, dbName, tableName);
        String embeddedMessage = embeddingInfo.getEmbeddingMessage();
        boolean anyMethodSuccess = false;

        // TODO: maybe not use this global table map, using meta data instead.
//        if (embeddedMessage.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_SIZE)
//            return new DBResponse(DBStatusCodes.CODE_TOO_LARGE_MESSAGE, DBStatusCodes.MSG_TOO_LARGE_MESSAGE);
        TableInfoWithPK tableInfoWithPK = tableMap.getInfoMap().get(identicalKey);
        if (tableInfoWithPK == null) {
            if (updateTableMap(systemId, dbName, tableName))
                tableInfoWithPK = tableMap.getInfoMap().get(identicalKey);
            else
                return new DBResponse(DBStatusCodes.CODE_NO_SOURCE_TABLE, DBStatusCodes.MSG_NO_SOURCE_TABLE);
        }

        boolean isAppend = false;
        boolean useColumn = true;
        Map<String, FieldModel.FieldUnit> columnsInfo = tableInfoWithPK.getTableInfo().getColumnsInfo();
        ArrayList<SelectedField> selectedFields = embeddingInfo.getSelectedFields();
        if (!embeddingInfo.isOwner()) {
            Parser.chooseEmbeddingFields(selectedFields, tableInfoWithPK); // Default, data user use columns to encode.
        } else if (selectedFields.size() == 0) {
            useColumn = false;
        }
        if (!useColumn && !embeddingInfo.isAllowColumnExpansion() && !embeddingInfo.isAllowRowExpansion())
            return new DBResponse(DBStatusCodes.CODE_NO_DW_METHOD_ALLOWED, DBStatusCodes.MSG_NO_DW_METHOD_ALLOWED);
        // 生成水印
        Watermark waterMark;
        try {
            waterMark = WaterMarkGenerator.generateWatermarkFromMessageWithKey(embeddedMessage, databaseService.getSystemKeyCodeById(systemId));
        } catch (WatermarkException e) {
            log.error("Fail to generate watermark:", e);
            return new DBResponse(DBStatusCodes.CODE_FAIL_TO_GENERATE_DW, DBStatusCodes.MSG_FAIL_TO_GENERATE_DW);
        }
        // 生成追溯信息
        BasicTraceInfo basicTraceInfo = BasicTraceInfo.builder()
                .embeddingMessage(embeddedMessage)
                .systemId(systemId)
                .dbName(dbName)
                .tableName(tableName)
                .watermark(waterMark)
                .allowColumnExpansion(embeddingInfo.isAllowColumnExpansion())
                .allowRowExpansion(embeddingInfo.isAllowRowExpansion())
                .useColumn(useColumn)
                .build();

        // 分段取数并嵌入水印，初始取数
        DatasetWithPK datasetWithPK = fetchDatasetByTableName(systemId, dbName, tableName, tableMap, embeddingInfo.getQuerySql());
        if (datasetWithPK == null) {
            return new DBResponse(DBStatusCodes.CODE_FETCH_DATA_FAIL, DBStatusCodes.MSG_FETCH_DATA_FAIL);
        }
        // For each field, choose encoder
        ArrayList<SelectedField> successFields = new ArrayList<>();
        ArrayList<ColExpansionInfo.ColExpansionSubAlgorithm> successColExpansionFields = new ArrayList<>();
        if (useColumn) {
            for (SelectedField selectedField :
                    selectedFields) {
                IEncoder encoder = parseEncoder(embeddingInfo, tableMap, selectedField);
                if (encoder == null) {
                    log.error(DBStatusCodes.MSG_INIT_ENCODER_FAIL);
                } else {
                    FieldModel fieldModel = new FieldModel();
                    DBResponse dbResponse = setFieldModel(fieldModel, tableInfoWithPK, selectedField.getFieldName());
                    if (dbResponse.getStatusCode() != DBStatusCodes.CODE_SUCCESS) return dbResponse;

                    basicTraceInfo.setFieldModel(fieldModel);
                    log.info("Start to embed watermark into Field [" + selectedField.getFieldName() + "]");
                    double startTime = System.currentTimeMillis();
                    // Get embedded data
                    try {
                        encoder.encode(datasetWithPK, basicTraceInfo);
                        successFields.add(selectedField);
                        anyMethodSuccess = true;
                    } catch (WatermarkException e) {
                        log.error("Watermark encoding fail.", e);
                    }
                    double endTime = System.currentTimeMillis();
                    log.info("Embedding costs: " + (endTime - startTime));
                }
            }
            // At least one field has been embedded.
            if (successFields.size() == 0)
                log.error(DBStatusCodes.MSG_EXCEPTION);
        }
        if (embeddingInfo.isAllowRowExpansion()) {
            ArrayList<RowExpansionInfo> rowExpansionInfos = new ArrayList<>();
            RowExpansionInfo rowExpansionInfo = RowExpansionInfo.builder()
                    .algorithm(WatermarkAlgorithm.ROW_SIMPLE_METHOD)
                    .maxCapacity(rowExpansionConf.getRowCapacityLimit())
                    .threshold(rowExpansionConf.getForgeRowNumThreshold())
                    .build();
            rowExpansionInfos.add(rowExpansionInfo);
            basicTraceInfo.setRowExpansionInfos(rowExpansionInfos);

            IRowEncoder rowEncoder = parseRowEncoder(rowExpansionConf);
            try {
                log.info("Start to embed row watermark.");
                double startTime = System.currentTimeMillis();
                rowEncoder.encode(datasetWithPK, basicTraceInfo);
                double endTime = System.currentTimeMillis();
                log.info("Embedding costs: " + (endTime - startTime));
                anyMethodSuccess = true;
            } catch (WatermarkException e) {
                log.error("Fail to encode row watermark. ", e);
            }
        }
        if (embeddingInfo.isAllowColumnExpansion()) {
            // All use default simple encoder.
            ArrayList<ColExpansionInfo> colExpansionInfos = new ArrayList<>();
            ColExpansionInfo colExpansionInfo = new ColExpansionInfo();
            ArrayList<ColExpansionInfo.ColExpansionSubAlgorithm> colExpansionSubAlgorithms = new ArrayList<>();
            ColExpansionInfo.ColExpansionSubAlgorithm colExpansionSubAlgorithm = new ColExpansionInfo.ColExpansionSubAlgorithm();
            colExpansionSubAlgorithms.add(colExpansionSubAlgorithm);
            colExpansionInfo.setSubAlgorithms(colExpansionSubAlgorithms);
            colExpansionInfo.setEncoder("");
            colExpansionInfos.add(colExpansionInfo);
            basicTraceInfo.setColumnExpansionInfos(colExpansionInfos);
            FieldModel fieldModel = new FieldModel();
            DBResponse dbResponse = setFieldModelOnlyWithPK(fieldModel, tableInfoWithPK);
            if (dbResponse.getStatusCode() != DBStatusCodes.CODE_SUCCESS) {
                log.error(dbResponse.getMessage());
            } else {
                basicTraceInfo.setFieldModel(fieldModel);
                for (ColExpansionInfo info : colExpansionInfos) {
                    basicTraceInfo.setColExpansionInfo(info);
                    IColEncoder iColEncoder = parseColExtEncoder(info);
                    if (iColEncoder == null) {
                        log.error("Fail to init column expansion encoder.");
                    } else {
                        try {
                            log.info("Start to embed extended column watermark.");
                            double startTime = System.currentTimeMillis();
                            iColEncoder.encode(datasetWithPK, basicTraceInfo);
                            double endTime = System.currentTimeMillis();
                            log.info("Embedding costs: " + (endTime - startTime));
                            anyMethodSuccess = true;
                            successColExpansionFields.addAll(basicTraceInfo.getColExpansionInfo().getSubAlgorithms());
                        } catch (WatermarkException e) {
                            log.error("Fail to encode column expansion watermark. ", e);
                        }
                    }
                }
            }
        }

        if (anyMethodSuccess) {
            // export to db
            if (embeddingInfo.isShouldOutputToDB()) {
                if (exportNewDataToDB(datasetWithPK, systemId, dbName, tableName, columnsInfo.size(), embeddingInfo.getOutputTable())) {
                    log.info("Save to database successfully.");
                } else {
                    return new DBResponse(DBStatusCodes.CODE_EXPORT_TO_NEW_TABLE_FAIL, DBStatusCodes.MSG_EXPORT_TO_NEW_TABLE_FAIL);
                }
            }
            // Always export to csv, as cache.
            String fileName = generateCSVFilePath(systemId, dbName, tableName);
            String dataId = generateDataID(systemId, dbName, tableName);
            if (dataId == null) return new DBResponse(StatusCodes.CODE_INTERNAL_ERROR, StatusCodes.MSG_INTERNAL_ERROR);
            cacheCSVFileMapping(systemId, dataId, fileName);
            ArrayList<String> colName = new ArrayList<>();
            datasetWithPK.getMetaData().forEach(fieldUnit -> colName.add(fieldUnit.getFieldName()));
            if (embeddingInfo.isAllowColumnExpansion() && !successColExpansionFields.isEmpty()) {
                // add new header to CSV
                successColExpansionFields.forEach(alg -> {
                    colName.add(alg.getFieldName());
                });
            }
            ArrayList<ArrayList<String>> finalDataSet = new ArrayList<>(datasetWithPK.getDataset().values());
            int sortedIndex = datasetWithPK.getSortedIndex();
            finalDataSet.sort(Comparator.comparing(a -> a.get(sortedIndex)));
            if (exportNewDataToLocalCSV(fileName, colName, finalDataSet, isAppend)) {
                log.info("Save as CSV file successfully.");
            } else {
                log.info("Fail to export db data as CSV file.");
                return new DBResponse(DBStatusCodes.CODE_EXPORT_TO_CSV_FILE_FAIL, DBStatusCodes.MSG_EXPORT_TO_CSV_FILE_FAIL);
            }

            // Record trace info
            if (!databaseService.recordTraceInfo(basicTraceInfo, successFields)) {
                log.error("Fail to record trace information to database.");
                return new DBResponse(StatusCodes.CODE_INTERNAL_ERROR, StatusCodes.MSG_INTERNAL_ERROR);
            }

            return DBResponse.builder()
                    .statusCode(DBStatusCodes.CODE_SUCCESS)
                    .message(DBStatusCodes.MSG_SUCCESS)
                    .dataId(dataId)
                    .dataSet(finalDataSet)
                    .build();
        }
        return new DBResponse(DBStatusCodes.CODE_NO_DW_METHOD_SUCCESS, DBStatusCodes.MSG_NO_DW_METHOD_SUCCESS);
    }

    /**
     * Extract watermark from a database table.
     * TODO: need fix.
     *
     * @param extractInfo database information
     */
    public DBResponse extractFromTable(ExtractInfo extractInfo) {
        String dbName = extractInfo.getDbName();
        String tableName = extractInfo.getTableName();
        String sourceTableName = parseSourceTable(tableName);
        if (sourceTableName == null) {
            return new DBResponse(DBStatusCodes.CODE_NO_SOURCE_TABLE, DBStatusCodes.MSG_NO_SOURCE_TABLE);
        }
        extractInfo.setSourceTableName(sourceTableName);
        IDecoder decoder = parseDecoder(extractInfo, tableMap);
        if (decoder == null) {
            return new DBResponse(DBStatusCodes.CODE_INIT_DECODER_FAIL, DBStatusCodes.MSG_INIT_DECODER_FAIL);
        }
        String newKey = generateIdenticalKey(extractInfo.getSystemId(), dbName, tableName);
        String oriKey = generateIdenticalKey(extractInfo.getSystemId(), dbName, sourceTableName);
        Map<String, TableInfoWithPK> tableInfoWithPKMap = tableMap.getInfoMap();
        if (!tableInfoWithPKMap.containsKey(newKey)) {
            tableInfoWithPKMap.put(newKey, tableInfoWithPKMap.get(oriKey));
            tableMap.setInfoMap(tableInfoWithPKMap);
        }

        FieldModel fieldModel = new FieldModel();
        TableInfoWithPK tableInfoWithPK = tableMap.getInfoMap().get(oriKey);
        DBResponse dbResponse = setFieldModel(fieldModel, tableInfoWithPK, extractInfo.getEmbeddingColumnName());
        if (dbResponse.getStatusCode() != DBStatusCodes.CODE_SUCCESS) return dbResponse;

        DatasetWithPK datasetWithPK = fetchDatasetByTableName(extractInfo.getSystemId(), dbName, tableName, tableMap, extractInfo.getQuerySql());
        if (datasetWithPK == null) {
            return new DBResponse(DBStatusCodes.CODE_FETCH_DATA_FAIL, DBStatusCodes.MSG_FETCH_DATA_FAIL);
        }

        // Generate trace info
        BasicTraceInfo basicTraceInfo = BasicTraceInfo.builder()
                .systemId(extractInfo.getSystemId())
                .dbName(dbName)
                .tableName(sourceTableName) // index by source table name
                .fieldModel(fieldModel)
                .build();
        return doExtract(decoder, datasetWithPK, basicTraceInfo);
    }

    // TODO: do not use CSV headers as the tracing components. Trying all possible algorithms and then calculating similarities is a better way.
    public DBResponse extractFromCSV(ExtractInfo extractInfo) {
        CSVUtils.CSVInfo csvInfo = readDataFromCSV(extractInfo.getFile());
        ArrayList<String> headers = csvInfo.getHeaders();
        boolean skipCol = false;
        if (headers == null) {
            log.info(DBStatusCodes.MSG_NO_CSV_HEADER);
            skipCol = true; // column can not be processed without headers.
        }
        ArrayList<TraceInfoEntity> traceInfoEntities = databaseService.getTraceInfo(extractInfo);
        if (traceInfoEntities == null || traceInfoEntities.isEmpty())
            return new DBResponse(DBStatusCodes.CODE_NO_SOURCE_TABLE, DBStatusCodes.MSG_NO_SOURCE_TABLE);
        ArrayList<WatermarkSimilarity> similarities = new ArrayList<>(traceInfoEntities.size());
        for (TraceInfoEntity traceInfoEntity :
                traceInfoEntities) {
            String dbName = traceInfoEntity.getDbName();
            String sourceTableName = traceInfoEntity.getTableName();

            String recordedWatermark = traceInfoEntity.getWatermark();
            String embeddedMessage = traceInfoEntity.getEmbeddedMsg();

            boolean allowRowExpansion = traceInfoEntity.getAllowRowExpansion().equals((byte) 1);
            boolean allowColumnExpansion = traceInfoEntity.getAllowColumnExpansion().equals((byte) 1) && !skipCol;
            boolean useColumn = false;
            Gson gson = new Gson();

            String wmFields = traceInfoEntity.getWmFields();
            if (wmFields != null) useColumn = !skipCol;
            BasicTraceInfo basicTraceInfo = BasicTraceInfo.builder()
                    .systemId(extractInfo.getSystemId())
                    .dbName(dbName)
                    .tableName(sourceTableName) // index by source table name
                    .build();

            DatasetWithPK datasetWithPK = new DatasetWithPK();
            ArrayList<FieldModel.FieldUnit> primaryKeys = new ArrayList<>();
            if ((useColumn || allowColumnExpansion)) {
                String pks = traceInfoEntity.getPrimaryKeys();
                if (pks != null) {
                    ArrayList<String> primaryKeyList = new ArrayList<>(Arrays.asList(pks.split(PRIMARY_KEY_DIVIDER)));
                    ArrayList<Integer> pkIndexList = new ArrayList<>();
                    for (String pk : primaryKeyList) {
                        FieldModel.FieldUnit pkField = new FieldModel.FieldUnit();
                        pkField.setFieldName(pk);
                        int pkIndex = -1;
                        for (int i = 0; i < headers.size(); ++i) {
                            if (pk.equals(headers.get(i))) {
                                pkIndex = i;
                                break;
                            }
                        }
                        if (pkIndex < 0) {
                            log.info("No matched primary key field.");
                        } else {
                            pkIndexList.add(pkIndex);
                            pkField.setFieldIndex(pkIndex);
                            pkField.setFieldType(COL_TYPE_TEXT);
                            primaryKeys.add(pkField);
                        }
                    }
                    Map<String, ArrayList<String>> dsMap = generateDataSetWithPK(csvInfo.getData(), pkIndexList);
                    if (dsMap != null) datasetWithPK.setDataset(dsMap);
                }
            }
            if (useColumn) {
                Type listType = new TypeToken<ArrayList<SelectedField>>() {
                }.getType();
                ArrayList<SelectedField> selectedFields = gson.fromJson(wmFields, listType);
                ArrayList<Double> aveSimilarity = new ArrayList<>(selectedFields.size());
                for (SelectedField selectedField :
                        selectedFields) {
                    String fieldName = selectedField.getFieldName();
                    int colIndex = -1;
                    for (int i = 0; i < headers.size(); ++i) {
                        if (fieldName.equals(headers.get(i))) {
                            colIndex = i;
                            break;
                        }
                    }
                    if (colIndex < 0)
                        continue;
                    FieldModel fieldModel = new FieldModel();
                    FieldModel.FieldUnit fieldUnit = new FieldModel.FieldUnit();
                    fieldUnit.setFieldIndex(colIndex);
                    fieldUnit.setFieldName(fieldName);
                    fieldModel.setSelectedField(fieldUnit);
                    fieldModel.setPrimaryKeys(primaryKeys);

                    extractInfo.setEmbeddingColumnName(fieldName);
                    extractInfo.setEmbeddingMethod(selectedField.getAlgorithm());
                    IDecoder decoder = parseDecoder(extractInfo);
                    if (decoder == null) // run others
                        log.error(DBStatusCodes.MSG_INIT_DECODER_FAIL);
                    else {
                        basicTraceInfo.setFieldModel(fieldModel);
                        DBResponse dbResponse = doExtract(decoder, datasetWithPK, basicTraceInfo);
                        if (dbResponse.getStatusCode() == DBStatusCodes.CODE_SUCCESS) {
                            String extractedWatermark = dbResponse.getExtractedMessage();
                            log.info("Current extracted watermark: " + extractedWatermark);
                            aveSimilarity.add(calcSimilarity(recordedWatermark, extractedWatermark));
                        }
                    }
                }
                if (aveSimilarity.size() != 0) {
                    double currentSimilarity = getFinalSimilarity(aveSimilarity);
                    WatermarkSimilarity watermarkSimilarity = new WatermarkSimilarity();
                    watermarkSimilarity.setSimilarity(currentSimilarity);
                    watermarkSimilarity.setMessage(embeddedMessage);
                    similarities.add(watermarkSimilarity);
                }
            }
            if (allowRowExpansion) {
                // pk may be useful
                datasetWithPK.setRawDataset(csvInfo.getData());
                String rowExpansionAlg = traceInfoEntity.getRowExpansionAlgorithm();
                if (rowExpansionAlg == null) {
                    log.info("Allow row expansion, but fail to find corresponding algorithms.");
                } else {
                    Type listType = new TypeToken<ArrayList<RowExpansionInfo>>() {
                    }.getType();
                    ArrayList<RowExpansionInfo> rowExpansionInfos = gson.fromJson(rowExpansionAlg, listType);
                    if (rowExpansionInfos == null) {
                        log.info("Allow row expansion, but fail to find corresponding parameters.");
                    } else {
                        ArrayList<Double> aveSimilarity = new ArrayList<>(rowExpansionInfos.size());
                        for (RowExpansionInfo rowExpansionInfo : rowExpansionInfos) {
                            basicTraceInfo.setRowExpansionInfo(rowExpansionInfo);
                            IRowDecoder decoder = parseRowDecoder(rowExpansionInfo);
                            if (decoder == null) {
                                log.error("Fail to init row decoder.");
                            } else {
                                log.info("Start to extract watermark from extended row.");
                                String extractedWatermark = decoder.decode(datasetWithPK, basicTraceInfo);
                                if (extractedWatermark != null) {
                                    log.info("Current row extracted watermark: " + extractedWatermark);
                                    aveSimilarity.add(calcSimilarity(recordedWatermark, extractedWatermark));
                                }
                            }
                        }
                        if (aveSimilarity.size() != 0) {
                            double currentSimilarity = getFinalSimilarity(aveSimilarity);
                            WatermarkSimilarity watermarkSimilarity = new WatermarkSimilarity();
                            watermarkSimilarity.setSimilarity(currentSimilarity);
                            watermarkSimilarity.setMessage(embeddedMessage);
                            similarities.add(watermarkSimilarity);
                        }
                    }
                }
            }
            if (allowColumnExpansion) {
                String colExpansionAlg = traceInfoEntity.getColumnExpansionAlgorithm();
                if (colExpansionAlg == null) {
                    log.info("Allow column expansion, but fail to find corresponding algorithms.");
                } else {
                    Type listType = new TypeToken<ArrayList<ColExpansionInfo>>() {
                    }.getType();
                    ArrayList<ColExpansionInfo> colExpansionInfos = gson.fromJson(colExpansionAlg, listType);
                    if (colExpansionInfos == null) {
                        log.info("Allow column expansion, but fail to find corresponding parameters.");
                    } else {
                        ArrayList<Double> aveSimilarity = new ArrayList<>(colExpansionInfos.size());
                        for (ColExpansionInfo colExpansionInfo :
                                colExpansionInfos) {
                            FieldModel fieldModel = new FieldModel();
                            fieldModel.setPrimaryKeys(primaryKeys);
                            basicTraceInfo.setFieldModel(fieldModel);
                            basicTraceInfo.setColExpansionInfo(colExpansionInfo);
                            basicTraceInfo.setTableHeaders(headers);

                            IColDecoder decoder = parseColExtDecoder(colExpansionInfo);
                            if (decoder == null)
                                log.error("Fail to init column expansion decoder.");
                            else {
                                log.info("Start to extract watermark from extended columns.");
                                String extractedWatermark = decoder.decode(datasetWithPK, basicTraceInfo);
                                if (extractedWatermark != null) {
                                    log.info("Current extended column extracted watermark: " + extractedWatermark);
                                    aveSimilarity.add(calcSimilarity(recordedWatermark, extractedWatermark));
                                }
                            }
                        }
                        if (aveSimilarity.size() != 0) {
                            double currentSimilarity = getFinalSimilarity(aveSimilarity);
                            WatermarkSimilarity watermarkSimilarity = new WatermarkSimilarity();
                            watermarkSimilarity.setSimilarity(currentSimilarity);
                            watermarkSimilarity.setMessage(embeddedMessage);
                            similarities.add(watermarkSimilarity);
                        }
                    }
                }
            }
        }
        if (similarities.size() == 0)
            return new DBResponse(DBStatusCodes.CODE_NO_DW_METHOD_SUCCESS, DBStatusCodes.MSG_NO_DW_METHOD_SUCCESS);
        similarities.sort((a, b) -> {
            //For Descending Order
            return b.getSimilarity().compareTo(a.getSimilarity());
        });
        // No matched watermark
        log.info("Current Threshold: " + threshold);
        if (similarities.get(0).getSimilarity() < threshold) {
            log.info("Too low similarity.");
            return DBResponse.builder()
                    .statusCode(DBStatusCodes.CODE_FAIL_TO_EXTRACT_DW)
                    .message(DBStatusCodes.MSG_FAIL_TO_EXTRACT_DW)
                    .build();
        }

        return DBResponse.builder()
                .statusCode(DBStatusCodes.CODE_SUCCESS)
                .message(DBStatusCodes.MSG_SUCCESS)
                .extractedMessage(similarities.get(0).getMessage()) // return the most similar one
                .build();
    }

    private DBResponse setFieldModel(FieldModel fieldModel, TableInfoWithPK tableInfoWithPK, String embeddingColumnName) {
        FieldModel.FieldUnit fieldUnit = null;
        ArrayList<FieldModel.FieldUnit> primaryKeys = new ArrayList<>();
        Map<String, FieldModel.FieldUnit> columnsInfo = tableInfoWithPK.getTableInfo().getColumnsInfo();
        ArrayList<String> columnName = tableInfoWithPK.getTableInfo().getColumnName();
        for (String colName :
                columnsInfo.keySet()) {
            if (colName.equals(embeddingColumnName)) {
                fieldUnit = columnsInfo.get(colName);
                break;
            }
        }
        if (fieldUnit != null) {
            fieldModel.setSelectedField(fieldUnit);
        } else {
            log.info("Cannot find the selected field in the table map.");
            return new DBResponse(DBStatusCodes.CODE_MISMATCHED_FIELD, DBStatusCodes.MSG_MISMATCHED_FIELD);
        }
        for (Integer i : tableInfoWithPK.getPkIndex()) {
            primaryKeys.add(columnsInfo.get(columnName.get(i)));
        }
        if (primaryKeys.isEmpty())
            return new DBResponse(DBStatusCodes.CODE_NO_PRIMARY_KEY, DBStatusCodes.MSG_NO_PRIMARY_KEY);
        fieldModel.setPrimaryKeys(primaryKeys);
        return new DBResponse(DBStatusCodes.CODE_SUCCESS, DBStatusCodes.MSG_SUCCESS);
    }

    private DBResponse setFieldModelOnlyWithPK(FieldModel fieldModel, TableInfoWithPK tableInfoWithPK) {
        FieldModel.FieldUnit fieldUnit = new FieldModel.FieldUnit();
        ArrayList<FieldModel.FieldUnit> primaryKeys = new ArrayList<>();
        Map<String, FieldModel.FieldUnit> columnsInfo = tableInfoWithPK.getTableInfo().getColumnsInfo();
        ArrayList<String> columnName = tableInfoWithPK.getTableInfo().getColumnName();
        fieldModel.setSelectedField(fieldUnit);
        for (Integer i : tableInfoWithPK.getPkIndex()) {
            primaryKeys.add(columnsInfo.get(columnName.get(i)));
        }
        if (primaryKeys.isEmpty())
            return new DBResponse(DBStatusCodes.CODE_NO_PRIMARY_KEY, DBStatusCodes.MSG_NO_PRIMARY_KEY);
        fieldModel.setPrimaryKeys(primaryKeys);
        return new DBResponse(DBStatusCodes.CODE_SUCCESS, DBStatusCodes.MSG_SUCCESS);
    }

    private DBResponse doExtract(IDecoder decoder, DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) {
        log.info("Start to extract watermark from Field[" + basicTraceInfo.getFieldModel().getSelectedField().getFieldName() + "]!");

        double startTime = System.currentTimeMillis();
        log.info(decoder.toString());
        String extractedMessage = decoder.decode(datasetWithPK, basicTraceInfo);
        double endTime = System.currentTimeMillis();
        log.info("Extracting costs：" + (endTime - startTime));
        if (extractedMessage == null) {
            log.info("Fail to extract inside.");
            return new DBResponse(DBStatusCodes.CODE_FAIL_TO_EXTRACT_DW, DBStatusCodes.MSG_FAIL_TO_EXTRACT_DW);
        }

        return DBResponse.builder()
                .statusCode(DBStatusCodes.CODE_SUCCESS)
                .message(DBStatusCodes.MSG_SUCCESS)
                .extractedMessage(extractedMessage)
                .build();
    }

    private double calcSimilarity(String recordedWatermark, String extractedWatermark) {
        if (extractedWatermark.length() != recordedWatermark.length()) {
            return -1.0; // invalid
        }
        int diffCounter = 0;
        int len = recordedWatermark.length();
        for (int i = 0; i < len; ++i) {
            if (recordedWatermark.charAt(i) != extractedWatermark.charAt(i)) {
                diffCounter++;
            }
        }
        return (len - diffCounter) * 1.0 / len;
    }

    private double getFinalSimilarity(ArrayList<Double> similarities) {
        return Collections.max(similarities);
    }
}