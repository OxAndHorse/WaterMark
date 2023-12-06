package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.integer;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.DecoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class IntegerDecoder implements IDecoder {
    public static final int LENGTH = 31;
    @Override
    public String decode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo){
        return CommonProcessorUtils.detectWatermark(datasetWithPK, basicTraceInfo, new DecoderCallableHandler());
    }

    static class DecoderCallableHandler implements DecoderCallable {
        @Override
        public boolean isZeroEmbedded(ArrayList<String> colValues) throws WatermarkException {
            int zeros = 0;
            int ones = 0;
            for (String s : colValues) {
                int intLength = s.length();
                int intValue = Integer.parseInt(s.substring(intLength-1));
                if((intLength == LENGTH+1 && intValue == 1) || (intLength == LENGTH && intValue == 0)){
                    zeros++;
                }
                else{
                    ones++;
                }
            }
            if (ones == zeros) throw new WatermarkException("Cannot distinguish the bit 0/1!");
            return zeros > ones;
        }
    }
}
