package cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.ts;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.processor.CommonCallable;

import java.sql.Timestamp;
import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.ts.Constants.PRECISION;

public class SimpleColExtTimestampDecoder extends CommonCallable {
    @Override
    public boolean isZeroEmbedded(ArrayList<String> colValues) throws WatermarkException {
        int zeros = 0;
        int ones = 0;
        int border = colValues.size() / 3; // over than 1/3
        int candidateCounter = 0;
        for (String s : colValues) {
            if (s == null) {
                candidateCounter++;
                continue;
            }
            try {
                Timestamp ts = Timestamp.valueOf(s);
                boolean isOdd = ((ts.getTime() / PRECISION) & 0x01) == 0x01;
                if (isOdd) ones++;
                else zeros++;
            } catch (IllegalArgumentException e) {
                candidateCounter++;
            }
        }
        if (candidateCounter > border) throw new WatermarkException("Too many illegal values");
        if (ones == zeros) throw new WatermarkException("Cannot distinguish the bit 0/1!");
        return zeros > ones;
    }

}
