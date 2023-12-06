package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.integer;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.EncoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.model.PartitionedDataset;
import cn.ac.iie.pkcgroup.dws.core.db.model.Watermark;
import cn.ac.iie.pkcgroup.dws.core.db.processor.CommonCallable;
import cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator;
import cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils;
import cn.ac.iie.pkcgroup.dws.core.db.utils.PartitionUtils;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Map;
@Slf4j
public class IntegerEncoder implements IEncoder {
    public static final int LENGTH = 31;
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
        public String generateDWString(String origin, boolean isZero){
            int intValue = Integer.parseInt(origin);
            String intString = Integer.toBinaryString(intValue);
            int len = intString.length();
            StringBuilder embeddedString = new StringBuilder(intString);
            for(int i = 0; i<LENGTH-len; i++){
                embeddedString.insert(0, '0');
            }

            boolean b = (isZero && intValue % 2 == 1) || (!isZero && intValue % 2 == 0);
            if(b){
                embeddedString.insert(0, '0');
            }
            return embeddedString.toString();


        }
    }
}
