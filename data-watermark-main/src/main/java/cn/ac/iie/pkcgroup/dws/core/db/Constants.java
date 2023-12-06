package cn.ac.iie.pkcgroup.dws.core.db;

import cn.ac.iie.pkcgroup.dws.comm.response.db.entity.AlgorithmInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator.WATERMARK_LENGTH;

@Slf4j
public class Constants {
    private static final String CONFIG_NAME = "config.properties";
    public static final String TRACE_INFO_TABLE = "trace_info";
    public static final String COL_TYPE_DOUBLE = "DOUBLE";
    public static final String COL_TYPE_INTEGER = "INTEGER";
    public static final String COL_TYPE_TEXT = "TEXT";
    public static final String COL_TYPE_TIMESTAMP = "TIMESTAMP";
    public static final String COL_NOT_SUPPORTED = "NOT_SUPPORTED";
    public static final String COL_NOT_USED = "NOT_USED";
    public static final String PRIMARY_KEY_DIVIDER = ",";
    public static final String DW_TABLE_SUFFIX = "_wm";
    public static final String CSV_MAPPING_DIVIDER = "|";
    public static final ArrayList<AlgorithmInfo> ALGORITHM_INFOS = new ArrayList<>();
    public static final Character ZERO = '\u200B';
    public static final Character ONE = '\u200C';
//    public static final int MAX_MESSAGE_SIZE = WATERMARK_LENGTH / Byte.SIZE;

    public static class WatermarkAlgorithm {
        public final static String NUMERIC_METHOD_DECIMAL = "N_LDP"; // "小数位奇偶算法";
        public final static String NUMERIC_METHOD_PATTERN_SEARCH = "N_PS"; // "模式搜索算法";
        public final static String TEXT_METHOD_SPACE = "T_UCE"; // ""字符嵌入算法";
        public final static String TEXT_METHOD_PUNCTUATION = "T_PUNC"; // "符号修改算法";
        public final static String ROW_SIMPLE_METHOD =  "R_SP";
        public final static String COL_EXT_SIMPLE_METHOD =  "C_SP";
        public final static String NUMERIC_METHOD_INTEGER =  "N_INT";
        public final static String NO_IMPLEMENTATION =  "CC"; // preserved

    }

    static {
        ALGORITHM_INFOS.add(new AlgorithmInfo(WatermarkAlgorithm.NUMERIC_METHOD_PATTERN_SEARCH, COL_TYPE_DOUBLE));
        ALGORITHM_INFOS.add(new AlgorithmInfo(WatermarkAlgorithm.TEXT_METHOD_SPACE, COL_TYPE_TEXT));
        ALGORITHM_INFOS.add(new AlgorithmInfo(WatermarkAlgorithm.TEXT_METHOD_PUNCTUATION, COL_TYPE_TEXT));
        ALGORITHM_INFOS.add(new AlgorithmInfo(WatermarkAlgorithm.NUMERIC_METHOD_INTEGER, COL_TYPE_INTEGER));
    }
}
