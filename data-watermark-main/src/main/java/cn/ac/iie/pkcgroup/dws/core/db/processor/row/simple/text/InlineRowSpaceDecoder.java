package cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.text;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.row.Constants.*;

public class InlineRowSpaceDecoder {

    public static String decode(String value) {
        if (value == null || value.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        char[] valueChars = value.toCharArray();
        if (valueChars[valueChars.length - 1] == ROW_FLAG) {
            for (int i = 0; i < valueChars.length - 1; ++i) {
                if (ZERO.equals(valueChars[i])) {
                    sb.append("0");
                } else if (ONE.equals(valueChars[i])) {
                    sb.append("1");
                }
            }
            return sb.toString();
        }
        return NO_DW;
    }
}
