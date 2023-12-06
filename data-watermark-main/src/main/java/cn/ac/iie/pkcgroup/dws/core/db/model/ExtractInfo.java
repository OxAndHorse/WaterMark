package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

@Data
public class ExtractInfo {
    private String systemId;
    private String dbName;
    private String tableName;
    private String sourceTableName;
    private String embeddingColumnName;
    private String embeddingMethod;
    private MultipartFile file; // only used when extracting csv
    private String querySql;
}
