package cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.ts;

public class Constants {
    public static final int PRECISION = 1000; // less than 1k would be ignored
    public static final long PERIOD = (long) PRECISION * 3600 * 24 * 365 * 5; // 5 years
}
