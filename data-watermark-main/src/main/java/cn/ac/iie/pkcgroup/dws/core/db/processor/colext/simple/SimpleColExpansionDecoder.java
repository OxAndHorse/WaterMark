package cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple;

import cn.ac.iie.pkcgroup.dws.core.db.interfaces.DecoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IColDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.ColExpansionInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.model.FieldModel;
import cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.ts.SimpleColExtTimestampDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils.getMostPossibleWatermark;

@Slf4j
public class SimpleColExpansionDecoder implements IColDecoder {
    @Override
    public String decode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) {
        ColExpansionInfo colExpansionInfo = basicTraceInfo.getColExpansionInfo();
        ArrayList<String> headers = basicTraceInfo.getTableHeaders();
        ArrayList<String> extractedWatermarks = new ArrayList<>();
        colExpansionInfo.getSubAlgorithms().forEach(alg -> {
            DecoderCallable decoderCallable;
            String fieldName = alg.getFieldName();
            int colIndex = -1;
            for (int i = 0; i < headers.size(); ++i) {
                if (fieldName.equals(headers.get(i))) {
                    colIndex = i;
                    break;
                }
            }
            if (colIndex < 0)
                return;
            FieldModel fieldModel = basicTraceInfo.getFieldModel();
            FieldModel.FieldUnit fieldUnit = new FieldModel.FieldUnit();
            fieldUnit.setFieldName(fieldName);
            fieldUnit.setFieldIndex(colIndex);
            fieldModel.setSelectedField(fieldUnit);
            basicTraceInfo.setFieldModel(fieldModel);
            int subAlgCode = alg.getSubAlg();
            if (subAlgCode < 0 || subAlgCode >= Constants.ExtColTypes.values().length) return;
            Constants.ExtColTypes extColTypes = Constants.ExtColTypes.values()[subAlgCode];
            switch (extColTypes) {
                case UUID:
                case DFT:
                    return;
                case TIMESTAMP:
                default:
                    decoderCallable = new SimpleColExtTimestampDecoder();
                    break;
            }
            log.info("Current extended field: [" + fieldName + "].");
            String extractedWatermark = CommonProcessorUtils.detectWatermark(datasetWithPK, basicTraceInfo, decoderCallable);
            extractedWatermarks.add(extractedWatermark);
        });
        if (extractedWatermarks.isEmpty()) return null;
        return getMostPossibleWatermark(extractedWatermarks);
    }
}
