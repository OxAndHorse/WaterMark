package cn.ac.iie.pkcgroup.dws.core.db.interfaces;

import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;

public interface IDecoder {
    /**
     * @apiNote 解码调用接口
     * @param datasetWithPK 数据集
     * @return 解码得到的水印
     */
    String decode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo);
}
