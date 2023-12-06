package cn.ac.iie.pkcgroup.dws.core.db.processor.text.space;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.DecoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.Constants.ONE;
import static cn.ac.iie.pkcgroup.dws.core.db.Constants.ZERO;

@Data
@Slf4j
public class SpaceDecoder implements IDecoder {
    @Override
    public String decode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) {
        return CommonProcessorUtils.detectWatermark(datasetWithPK, basicTraceInfo, new DecoderCallableHandler());
//        return StringUtils.decodeFromBitString(decodedWatermark);
    }

    static class DecoderCallableHandler implements DecoderCallable {
        @Override
        public boolean isZeroEmbedded(ArrayList<String> colValues) throws WatermarkException {
            int zeros = 0;
            int ones = 0;
            int border = colValues.size() / 3; // over than 1/3
            int candidateCounter = 0;
            for (String s : colValues) {
                char[] chars = s.toCharArray();
                if (chars.length == 0) {
                    candidateCounter++;
                    continue;
                }
                char lastChar = chars[chars.length - 1];
                if (ZERO.equals(lastChar))
                    zeros++;
                else if (ONE.equals(lastChar))
                    ones++;
                else
                    candidateCounter++;
            }
            if (candidateCounter > border) throw new WatermarkException("Too many NULL values");
            if (ones == zeros) throw new WatermarkException("Cannot distinguish the bit 0/1!");
            return zeros > ones;
        }
    }
}
