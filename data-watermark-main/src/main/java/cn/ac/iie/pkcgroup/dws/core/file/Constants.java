package cn.ac.iie.pkcgroup.dws.core.file;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final String TYPE_PDF = "PDF";
    public static final String TYPE_WORD_X = "DOCX";
    public static final String TYPE_NOT_SUPPORTED = "NOT_SUPPORTED";

    public static final String DW_FILE_SUFFIX = "_wm";

    public static final int WORD_UNIT = 1; // one bit
    public static final Character WORD_ZERO = '\u200B';
    public static final Character WORD_ONE = '\u200C';
    public static final String[] WORD_ENCODING_MAP = {WORD_ZERO.toString(), WORD_ONE.toString()};
    public static Map<Character, Integer> WORD_DECODING_MAP = new HashMap<>();

    public static int WORD_VISIBLE_WORD_WIDTH = 35;
    public static int WORD_VISIBLE_WORD_HEIGHT = 40;
    public static int WORD_VISIBLE_WORD_FOOT_TEXT_SIZE = 10;
    public static int WORD_VISIBLE_WORD_FOOT_WIDTH = WORD_VISIBLE_WORD_FOOT_TEXT_SIZE + 1;
    public static int WORD_VISIBLE_WORD_FOOT_HEIGHT = WORD_VISIBLE_WORD_FOOT_TEXT_SIZE + 5;

    static {
        WORD_DECODING_MAP.put(WORD_ZERO, 0x00);
        WORD_DECODING_MAP.put(WORD_ONE, 0x01);
    }
}
