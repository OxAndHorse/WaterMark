package cn.ac.iie.pkcgroup.dws.core.file.interfaces;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileExtractInfo;

public interface IDecoder {
    String decode(FileExtractInfo fileExtractInfo) throws WatermarkException;
}
