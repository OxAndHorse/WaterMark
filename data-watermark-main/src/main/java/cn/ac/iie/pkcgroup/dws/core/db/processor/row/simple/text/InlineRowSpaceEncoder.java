package cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.text;

import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.row.Constants.*;

public class InlineRowSpaceEncoder {

    public static String encode(String value, ArrayList<Integer> bits) {
        if (value == null || value.isEmpty()) return null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(value);
        for (Integer bit : bits) {
            stringBuilder.append(bit == 0 ? ZERO : ONE);
        }
        stringBuilder.append(ROW_FLAG);
        return stringBuilder.toString();
    }
}
