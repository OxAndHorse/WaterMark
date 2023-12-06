package cn.ac.iie.pkcgroup.dws.comm.request.db;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DBEmbedRequest {
    @NotNull(message = "dbName should not be null.")
    public String dbName;
    @NotNull(message = "tableName should not be null.")
    public String tableName;
    public SelectedField[] selectedFields;

    public boolean shouldOutputToDB;
    public String outputTable;
    public String embeddedMessage;
    public String querySql;
    public boolean allowRowExpansion;
    public boolean allowColumnExpansion;
}
