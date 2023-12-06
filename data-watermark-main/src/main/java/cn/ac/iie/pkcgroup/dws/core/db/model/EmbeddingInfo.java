package cn.ac.iie.pkcgroup.dws.core.db.model;

import cn.ac.iie.pkcgroup.dws.comm.request.db.SelectedField;
import lombok.Data;

import java.util.ArrayList;

@Data
public class EmbeddingInfo {
    private String systemId;
    private String dbName;
    private String tableName;
    private ArrayList<SelectedField> selectedFields;
    private FieldModel fieldModel;
    private String embeddingMethod;
    private String embeddingMessage;
    private boolean shouldOutputToDB;
    private String outputTable;
    private boolean isOwner;
    private String querySql;

    private boolean allowRowExpansion;
    private boolean allowColumnExpansion;
}
