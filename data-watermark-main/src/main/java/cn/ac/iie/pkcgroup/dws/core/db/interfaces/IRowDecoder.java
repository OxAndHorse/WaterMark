package cn.ac.iie.pkcgroup.dws.core.db.interfaces;

import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.Dataset;

public interface IRowDecoder {
    String decode(Dataset dataset, BasicTraceInfo basicTraceInfo);
}
