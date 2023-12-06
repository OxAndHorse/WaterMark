package cn.ac.iie.pkcgroup.dws.core.db.interfaces;

import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;

public interface IEncoder {
    /**
     * @apiNote 带主键的数据集嵌入水印
     * @param datasetWithPK	带主键的数据集
     */
    void encode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) throws WatermarkException;

    // 获取嵌入后得到的密参信息
    // 用于存回数据库
//    StoredKey getStoredKey();

//    void setPKIndex(int pkIndex);
//    void setEmbedColIndex(int embedColIndex);
//
//    void setStoredKeyBuilder(StoredKey.Builder storedKeyBuilder);
//    void setDataConstraint(ColumnDataConstraint dataConstraint);
}
