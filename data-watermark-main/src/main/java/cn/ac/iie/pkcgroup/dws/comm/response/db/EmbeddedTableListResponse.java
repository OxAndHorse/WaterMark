package cn.ac.iie.pkcgroup.dws.comm.response.db;

import cn.ac.iie.pkcgroup.dws.core.db.model.EmbeddedTableInfo;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class EmbeddedTableListResponse extends BasicResponse {
    ArrayList<EmbeddedTableInfo> embeddedTableList;
}
