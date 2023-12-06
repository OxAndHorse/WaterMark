package cn.ac.iie.pkcgroup.dws.core.db.processor.text.punc;

import cn.ac.iie.pkcgroup.dws.core.db.interfaces.DecoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.text.punc.PunctuationUtils.isPunctuation;

@Data
@Slf4j
public class PunctuationDecoder implements IDecoder {
    @Override
    public String decode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) {
        String decodedWatermark = CommonProcessorUtils.detectWatermark(datasetWithPK, basicTraceInfo, new DecoderCallableHandler());
        if (decodedWatermark == null) {
            return null;
        }
        return StringUtils.decodeFromBitString(decodedWatermark);
    }

    static class DecoderCallableHandler implements DecoderCallable {
        @Override
        public boolean isZeroEmbedded(ArrayList<String> colValues) {
            int zeros = 0;
            int len = colValues.size();
            for (String s : colValues) {
                int cnt = getPunctuation(s);
                if (cnt % 2 == 0)
                    zeros++;
            }
            return zeros > len / 2;
        }

        private int getPunctuation(String s) {
            StringBuilder tmp = new StringBuilder(s);
            int len = tmp.length();
            int cnt = 0;
            for (int i = 0; i < len; i++) {
                if (isPunctuation(tmp.charAt(i))) {
                    cnt++;
                }
            }
            return cnt;
        }
    }
}
