package cn.ac.iie.pkcgroup.dws.service;

import cn.ac.iie.pkcgroup.dws.config.model.ConfigUnit;
import cn.ac.iie.pkcgroup.dws.config.model.TableConfigs;
import cn.ac.iie.pkcgroup.dws.data.table.TableInfoWithPK;
import cn.ac.iie.pkcgroup.dws.data.table.TableMap;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

@Service
@Slf4j
public class InitService {
    private TableConfigs config;
    Map<String, DataSource> connections;
    DatabaseService databaseService;


    @Autowired
    public void setConfig(@Qualifier("tableConfigs") TableConfigs tableConfigs) {
        config = tableConfigs;
    }

    @Autowired
    public void setConnections(DatabaseService.DBConnectionMap connectionMap) {
        connections = connectionMap.getDataSourceMap();
    }

    @Autowired
    public void setDatabaseService(DatabaseService service) {
        databaseService = service;
    }


    // TODO: dynamic generation
    @Bean(value = "tableMap")
    @DependsOn({"jdbcConnections", "tableConfigs"})
    public TableMap getTableInfo() {
        TableMap tableMap = new TableMap();
        Map<String, TableInfoWithPK> map = tableMap.getInfoMap();
        Connection conn = null;
        for (String key : // key = systemId.dbName
                connections.keySet()) {
            try {
                DataSource ds = connections.get(key);
                conn = ds.getConnection();
                DatabaseMetaData databaseMetaData = conn.getMetaData();
                ArrayList<String> tableLists = parseTableNameBySystemIdAndDBName(key);
                for (String tableName :
                        tableLists) {
                    ResultSet tabs = databaseMetaData.getTables(null, null, tableName, null);
                    while (tabs.next()) {
                        if (tabs.getString("TABLE_NAME").equals(tableName)) {
                            TableInfoWithPK tableInfoWithPK = databaseService.getTableInfoWithPK(tableName, databaseMetaData, tabs);
                            String identicalKey = StringUtils.generateIdenticalKey(key, tableName);
                            map.put(identicalKey, tableInfoWithPK);
                        }
                    }
                }
                conn.close();
            } catch (SQLException e) {
                log.error("Get metadata fail: " + e.getMessage());
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        log.error("Unable to close connection:", ex);
                    }
                }
                return null;
            }
        }
        return tableMap;
    }


    private ArrayList<String> parseTableNameBySystemIdAndDBName(String connectionKey) {
        ArrayList<String> tableNames = new ArrayList<>(1);
        ArrayList<String> list = StringUtils.parseAllPartsFromIdenticalKey(connectionKey);
        String systemId = list.get(0);
        String dbName = list.get(1);
        for (ConfigUnit configUnit :
                config.getConfigs()) {
            if (configUnit.getDbName().equals(dbName) && configUnit.getSystemId().equals(systemId)) {
                tableNames.add(configUnit.getTableName());
            }
        }
        return tableNames;
    }
}
