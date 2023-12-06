package cn.ac.iie.pkcgroup.dws.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class CacheService {
    @Value("${conf.mappingRoot}")
    String mappingRoot;
    @Value("${conf.selectedTables}")
    String selectedTables;
    @Value("${conf.csvMappingRoot}")
    String csvMappingRoot;
    @Value("${conf.tmpRoot}")
    private String tmpRoot;
    @Value("${conf.fileRoot}")
    private String fileRoot;
    @Value("${conf.property}")
    private String propertyFilePath;

    private DB initCache(String cachePath, String type) {
        DB db = null;
        String successTemplate = "Init local {%s} cache successfully.";
        String noDirTemplate = "Fail to init local {%s} cache since the directory does not exist.";
        String exceptionTemple = "Fail to init local {%s} db.";
        try {
            DBFactory factory = new Iq80DBFactory();
            Options options = new Options();
            options.createIfMissing(true);
            File file = new File(cachePath);
            if (!file.exists()) {
                if (file.mkdirs())
                    db = factory.open(file, options);
                else {
                    log.error(String.format(noDirTemplate, type));
                    return null;
                }
            } else {
                db = factory.open(file, options);
            }
            log.info(String.format(successTemplate, type));
        } catch (Exception e) {
            log.error(String.format(exceptionTemple, type));
        }
        return db;
    }

    @Bean(value = "pathInit")
    public PathInitResult initPath() {
        PathInitResult pathInitResult = new PathInitResult();
        try {
            File tmpPath = new File(tmpRoot);
            File fileRootPath = new File(fileRoot);
            if (!tmpPath.exists()) {
                if (tmpPath.mkdirs()) {
                    log.info("Init temp root successfully.");
                    pathInitResult.setTmpRootInitialized(true);
                }
                else log.error("Fail to init temp root.");
            }
            if (!fileRootPath.exists()) {
                if (fileRootPath.mkdirs()) {
                    log.info("Init file root successfully.");
                    pathInitResult.setFileRootInitialized(true);
                }
                else log.error("Fail to init file root.");
            }
            File property = new File(propertyFilePath);
            if (!property.exists()) {
                File dir = property.getParentFile();
                if (!dir.exists()) {
                    if (!dir.mkdirs()) log.error("Fail to init directory of the persistence property file");
                }
                if (property.createNewFile()) {
                    log.info("Create persistence property file successfully.");
                    pathInitResult.setPropertyInitialized(true);
                }
                else log.error("Fail to create persistence property file.");
            }
        } catch (Exception e) {
            log.error("Fail to init paths.", e);
        }
        return pathInitResult;
    }

    @Bean(value = "fileMapping")
    public DB initFileMapping() {
        return initCache(mappingRoot, "file mapping");
    }

    @Bean(value = "selectedTableCache")
    public DB initSelectedTableCache() {
        return initCache(selectedTables, "selected table mapping");
    }

    @Bean(value = "csvFileMapping")
    public DB initCSVFileCache() {
        return initCache(csvMappingRoot, "CSV file mapping");
    }

    @Data
    public static class PathInitResult {
        boolean isTmpRootInitialized;
        boolean isFileRootInitialized;
        boolean isPropertyInitialized;
    }
}
