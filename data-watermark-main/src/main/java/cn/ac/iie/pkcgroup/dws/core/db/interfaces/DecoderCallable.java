package cn.ac.iie.pkcgroup.dws.core.db.interfaces;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;

import java.util.ArrayList;

public interface DecoderCallable {
    boolean isZeroEmbedded(ArrayList<String> colValues) throws WatermarkException;
}
