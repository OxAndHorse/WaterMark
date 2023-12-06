package cn.ac.iie.pkcgroup.dws.core.db.processor.text.punc;

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

import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.text.punc.PunctuationUtils.isPunctuation;

@Slf4j
public class PunctuationEncoder implements IEncoder {
    @Override
    public void encode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) throws WatermarkException {
        String secretKey = PartitionUtils.generatePartitionKey(basicTraceInfo);
        PartitionedDataset partitionedDataset = PartitionUtils.divide(datasetWithPK, secretKey);
        partitionedDataset.setTotalCount(datasetWithPK.getDataset().size());

        Watermark waterMark = WaterMarkGenerator.generateWatermarkFromMessage(basicTraceInfo.getEmbeddingMessage());
        int colIndex = basicTraceInfo.getFieldModel().getSelectedField().getFieldIndex();
        CommonProcessorUtils.encodeAllBits(partitionedDataset, waterMark.getBinary(), colIndex, new EncoderCallableHandler());
    }


    static class EncoderCallableHandler extends CommonCallable {
        @Override
        public String generateDWString(String origin, boolean isZero) {
            if (origin == null) return null;
            StringBuilder sb = new StringBuilder(origin);
            int len = sb.length();
            int cnt = 0;
            boolean isPunc = true;
            for (int i = 0; i < len; i++) {
                //count the sum of punctuation.
                if (isPunctuation(sb.charAt(i))) {
                    cnt++;
                    isPunc = true;
                } else {
                    isPunc = false;
                }
            }
            boolean b = (isZero && cnt % 2 == 1) || (!isZero && cnt % 2 == 0);
            if (isPunc) {
                if (b) {
                    sb.deleteCharAt(len - 1);
                }
            } else {
                if (b) {
                    sb.append('.');
                }
            }
            return sb.toString();
        }
    }
}
