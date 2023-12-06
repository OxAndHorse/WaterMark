package cn.ac.iie.pkcgroup.dws.core.db.response;

import cn.ac.iie.pkcgroup.dws.core.db.model.EmbeddedTableInfo;
import cn.ac.iie.pkcgroup.dws.response.WatermarkResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Data
@SuperBuilder
public class DBResponse extends WatermarkResponse {
    ArrayList<ArrayList<String>> dataSet;
    String dataId;
    byte[] csvFile;
    String csvFileName;
    String extractedMessage;
    ArrayList<EmbeddedTableInfo> embeddedTableList;

    public DBResponse(int code, String msg) {
        super(code, msg);
    }
    // Reserved for unique response
}
