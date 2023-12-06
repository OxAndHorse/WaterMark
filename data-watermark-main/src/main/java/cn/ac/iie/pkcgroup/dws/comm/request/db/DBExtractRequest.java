package cn.ac.iie.pkcgroup.dws.comm.request.db;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DBExtractRequest {
    @NotNull(message = "systemId should not be null.")
    public String systemId;
    @NotNull(message = "dbName should not be null.")
    public String dbName;
    @NotNull(message = "tableName should not be null.")
    public String tableName;
//    @NotNull(message = "selected fields should not be null.")
    public SelectedField[] selectedFields;
}
