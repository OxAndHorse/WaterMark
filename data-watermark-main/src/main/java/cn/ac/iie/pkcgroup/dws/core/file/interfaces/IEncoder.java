package cn.ac.iie.pkcgroup.dws.core.file.interfaces;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileEmbeddingInfo;

public interface IEncoder {
    void encode(FileEmbeddingInfo fileEmbeddingInfo) throws WatermarkException;
}
