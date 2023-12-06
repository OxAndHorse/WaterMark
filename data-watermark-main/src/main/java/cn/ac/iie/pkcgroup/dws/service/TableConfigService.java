package cn.ac.iie.pkcgroup.dws.service;

import cn.ac.iie.pkcgroup.dws.config.model.ConfigUnit;
import cn.ac.iie.pkcgroup.dws.config.model.TableConfigs;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

@Service
@Slf4j
public class TableConfigService {
    private DB tableMapping;
    @Autowired
    public void setTableMapping(@Qualifier("selectedTableCache") DB db) {
        tableMapping = db;
    }

    @Bean(value = "tableConfigs")
    @DependsOn(value = "selectedTableCache")
    public TableConfigs initTableConfigs() {
        ArrayList<ConfigUnit> configUnitList = new ArrayList<>();
        for (Map.Entry<byte[], byte[]> kv : tableMapping) {
            byte[] tableConfigsBytes = kv.getKey();
            if (tableConfigsBytes.length > 0) {
                String strJson = new String(tableConfigsBytes, StandardCharsets.UTF_8);
                Gson gson = new Gson();
                ConfigUnit configUnit = gson.fromJson(strJson, ConfigUnit.class);
                if (configUnit != null) {
                    configUnitList.add(configUnit);
                }
            }
        }
        TableConfigs tableConfigs = new TableConfigs();
        tableConfigs.setConfigs(configUnitList);
        return tableConfigs;
    }
}
