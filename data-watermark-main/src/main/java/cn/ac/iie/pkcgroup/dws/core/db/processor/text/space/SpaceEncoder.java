package cn.ac.iie.pkcgroup.dws.core.db.processor.text.space;

import cn.ac.iie.pkcgroup.dws.core.db.interfaces.EncoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.model.PartitionedDataset;
import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.model.Watermark;
import cn.ac.iie.pkcgroup.dws.core.db.processor.CommonCallable;
import cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator;
import cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils;
import cn.ac.iie.pkcgroup.dws.core.db.utils.PartitionUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.Constants.ONE;
import static cn.ac.iie.pkcgroup.dws.core.db.Constants.ZERO;

@Data
@Slf4j
public class SpaceEncoder implements IEncoder {
    @Override
    public void encode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) throws WatermarkException {
        String secretKey = PartitionUtils.generatePartitionKey(basicTraceInfo);
        PartitionedDataset partitionedDataset = PartitionUtils.divide(datasetWithPK, secretKey);
        partitionedDataset.setTotalCount(datasetWithPK.getDataset().size());

        Watermark waterMark = basicTraceInfo.getWatermark();
        int colIndex = basicTraceInfo.getFieldModel().getSelectedField().getFieldIndex();
        CommonProcessorUtils.encodeAllBits(partitionedDataset, waterMark.getBinary(), colIndex, new EncoderCallableHandler());
    }

    static class EncoderCallableHandler extends CommonCallable {
        @Override
        public String generateDWString(String origin, boolean isZero) {
            if (origin == null) return null;
            return isZero ? origin + ZERO : origin + ONE;
        }
    }
}
