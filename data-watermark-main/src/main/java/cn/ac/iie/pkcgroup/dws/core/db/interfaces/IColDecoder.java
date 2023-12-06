package cn.ac.iie.pkcgroup.dws.core.db.interfaces;

import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;

public interface IColDecoder {
    String decode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo);
}
