package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.pad;

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

import static cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.pad.Constants.EMBED_COUNT;

@Slf4j
public class DecimalPadEncoder implements IEncoder {
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
        /**
         * @param doubleString A double-string
         * @param isZero       Is bit zero
         * @return embedded 0/1 bit double-string
         */
        public String generateDWString(String doubleString, boolean isZero) {
            StringBuilder sb = new StringBuilder();
            String pointer = ".";
            try {
                Double.valueOf(doubleString);
                int pointerIndex = doubleString.indexOf(pointer);
                if (pointerIndex < 0) {
                    sb.append(doubleString).append(pointer);
                    sb.append(generateRandomNumberString(EMBED_COUNT, isZero)); // bit 0: embed an odd number
                } else {
                    sb.append(doubleString, 0, pointerIndex + 1);
                    String decimalString = doubleString.substring(pointerIndex + 1);
                    sb.append(changeDecimalString(decimalString, isZero));
                }
                return sb.toString();
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private String changeDecimalString(String decimalString, boolean isOdd) {
            int decimalValue = Integer.parseInt(decimalString);
            if (isOdd && decimalValue % 2 == 1 || !isOdd && decimalValue % 2 == 0) {
                return decimalString;
            }
            SecureRandom secureRandom = new SecureRandom();
            int changedValue = secureRandom.nextBoolean() || decimalValue == 0 ? decimalValue + 1 : decimalValue - 1; // odd->even, even->odd
            StringBuilder sb = new StringBuilder();
            String res = Integer.toString(changedValue);
            if (res.length() < decimalString.length()) {
                for (int i = 0; i < decimalString.length() - res.length(); ++i) {
                    sb.append("0");
                }
            }
            sb.append(res);
            return sb.toString();
        }

        private String generateRandomNumberString(int length, boolean isOdd) {
            SecureRandom secureRandom = new SecureRandom();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                int b = secureRandom.nextInt(10);
                while (isOdd && b % 2 != 1 || !isOdd && b % 2 == 0) {
                    b = secureRandom.nextInt(10);
                }
                sb.append(b);
            }
            return sb.toString();
        }
    }
}
