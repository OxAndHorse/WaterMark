package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.pad;

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
public class DecimalPadDecoder implements IDecoder {
    @Override
    public String decode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) {
        Handler handler = new Handler();
        return CommonProcessorUtils.detectWatermark(datasetWithPK, basicTraceInfo, handler);
    }

    static class Handler implements DecoderCallable {
        @Override
        public boolean isZeroEmbedded(ArrayList<String> colValues) throws WatermarkException {
            String pointer = ".";
            int zeros = 0;
            int ones = 0;
            int candidateCounter = 0;
            int border = colValues.size() / 3; // over than 1/3
            for (String s : colValues) {
                try {
                    Double.valueOf(s);
                    int pointerIndex = s.indexOf(pointer);
                    if (pointerIndex > 0) {
                        String decimalString = s.substring(pointerIndex + 1);
                        int decimalValue = Integer.parseInt(decimalString);
                        if (decimalValue % 2 == 1) zeros++;
                        else ones++;
                    }
                } catch (NumberFormatException e) {
                    log.error("Illegal format of double field!");
                    candidateCounter++;
                }
            }
            if (candidateCounter > border) throw new WatermarkException("Too many illegal double values!");
            if (ones == zeros) throw new WatermarkException("Cannot distinguish the bit 0/1!");
            return zeros > ones;
        }
    }

}
