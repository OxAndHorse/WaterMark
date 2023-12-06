package cn.ac.iie.pkcgroup.dws.core.db.interfaces;

public interface EncoderCallable {
    String generateDWString(String origin, boolean isZero);
    String generateDWString(boolean isZero);
}