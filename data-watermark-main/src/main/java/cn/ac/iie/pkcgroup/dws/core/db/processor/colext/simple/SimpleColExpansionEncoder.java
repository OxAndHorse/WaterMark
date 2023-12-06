package cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.EncoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IColEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.*;
import cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.ts.SimpleColExtTimestampEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils;
import cn.ac.iie.pkcgroup.dws.core.db.utils.PartitionUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.sql.Types;
import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.Constants.ExtColTypes.TIMESTAMP;
import static cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.Constants.TS_SUFFIX;

public class SimpleColExpansionEncoder implements IColEncoder {
    @Override
    public void encode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) throws WatermarkException {
        ArrayList<FieldModel.FieldUnit> metaData = datasetWithPK.getMetaData();
        ColExpansionInfo colExpansionInfo = basicTraceInfo.getColExpansionInfo();
        ArrayList<ColExpansionInfo.ColExpansionSubAlgorithm> subAlgorithms = colExpansionInfo.getSubAlgorithms();

        Watermark waterMark = basicTraceInfo.getWatermark();
        EncoderCallable encoderCallable;
        for (ColExpansionInfo.ColExpansionSubAlgorithm subAlgorithm : subAlgorithms) {
            int subAlgCode = subAlgorithm.getSubAlg();
            if (subAlgCode < 0 || subAlgCode >= Constants.ExtColTypes.values().length) return;
            Constants.ExtColTypes extColTypes = Constants.ExtColTypes.values()[subAlgCode];
            String extColName;
            int fieldType;
            switch (extColTypes) {
                case UUID:
                case DFT:
                    return;
                case TIMESTAMP:
                default:
                    extColName = generateRandomExtColumnName(metaData, TIMESTAMP);
                    fieldType = Types.TIMESTAMP;
                    encoderCallable = new SimpleColExtTimestampEncoder();
                    break;
            }
            subAlgorithm.setFieldName(extColName);
            subAlgorithm.setFieldType(fieldType);

            FieldModel fieldModel = basicTraceInfo.getFieldModel();
            FieldModel.FieldUnit fieldUnit = fieldModel.getSelectedField();
            fieldUnit.setFieldName(extColName);
            basicTraceInfo.setFieldModel(fieldModel);

            String secretKey = PartitionUtils.generatePartitionKey(basicTraceInfo);
            PartitionedDataset partitionedDataset = PartitionUtils.divide(datasetWithPK, secretKey);
            partitionedDataset.setTotalCount(datasetWithPK.getDataset().size());
            CommonProcessorUtils.encodeAllBits(partitionedDataset, waterMark.getBinary(), encoderCallable);
        }
    }

    private String generateRandomExtColumnName(ArrayList<FieldModel.FieldUnit> metaData, Constants.ExtColTypes types) {
        String colName;
        boolean isValid;
        do {
            isValid = true;
            switch (types) {
                case TIMESTAMP:
                    colName = RandomStringUtils.randomAlphanumeric(4) + TS_SUFFIX;
                    break;
                case UUID:
                case DFT:
                default:
                    return null;
            }
            for (FieldModel.FieldUnit fieldUnit : metaData) {
                if (fieldUnit.getFieldName().equals(colName)) { // existed column
                    isValid = false;
                    break;
                }
            }
        } while (!isValid);
        return colName;
    }
}
