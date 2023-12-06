package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@Data
@SuperBuilder
@NoArgsConstructor
public class BasicTraceInfo {
    private String systemId;
    private Watermark watermark;
    private String dbName;
    private String tableName;
    private String embeddingMessage;
    // PK和嵌入字段的信息，包括字段索引、字段名
    private FieldModel fieldModel;

    private boolean useColumn;
    private boolean allowRowExpansion;
    private ArrayList<RowExpansionInfo> rowExpansionInfos;
    private boolean allowColumnExpansion;
    private ArrayList<ColExpansionInfo> columnExpansionInfos;

    private RowExpansionInfo rowExpansionInfo;
    private ColExpansionInfo colExpansionInfo;

    private ArrayList<String> tableHeaders;
}
