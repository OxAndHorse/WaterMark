package cn.ac.iie.pkcgroup.dws.core.db.processor;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.DecoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.EncoderCallable;

import java.util.ArrayList;

public class CommonCallable implements EncoderCallable, DecoderCallable {
    @Override
    public String generateDWString(String origin, boolean isZero){
        return null;
    }

    @Override
    public String generateDWString(boolean isZero) {
        return null;
    }

    @Override
    public boolean isZeroEmbedded(ArrayList<String> colValues) throws WatermarkException {
        return false;
    }
}
