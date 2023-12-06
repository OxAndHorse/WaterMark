package cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.numeric;

import cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.SimpleRowEncoder.AnalyzedField;
import cn.ac.iie.pkcgroup.dws.core.db.utils.row.NumericStatisticUtils.NumericStatisticValue;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Stack;

import static cn.ac.iie.pkcgroup.dws.core.db.utils.row.NumericStatisticUtils.DOT;

/**
 * Actually, we do not use this encoder for single/double precision values...
 */
@Slf4j
public class InlineRowPadEncoder {

    public static String encode(String material, AnalyzedField analyzedField, ArrayList<Integer> bits, int limit) {
        if (material == null) return null;
        if (bits.isEmpty()) return material; // not process
        boolean isNegative = material.startsWith("-");
        NumericStatisticValue numericStatisticValue = (NumericStatisticValue) analyzedField.getAnalyzedValue();
        int decimalNum = numericStatisticValue.getAveDecimalNum();

        ArrayList<ArrayList<Integer>> groupedBits = generateGroupedBits(bits, limit);
        Stack<String> stack = new Stack<>();
        double forgeValue;
        do {
            StringBuilder sb = new StringBuilder();
            if (isNegative) sb.append("-"); // negative
            int processNum = 0;
            for (ArrayList<Integer> unit : groupedBits) {
                if (processNum == decimalNum) stack.push(DOT);
                int value = 0;
                for (int i = 0; i < unit.size(); ++i) {
                    value += (unit.get(i) << (unit.size() - 1 - i)) & 0x07;
                }
                stack.push(String.valueOf(value + 1));
                processNum++;
            }
            if (processNum < decimalNum) {
                for (int i = 0; i < decimalNum - processNum; i++) {
                    stack.push("0"); // fill with 0: 0.00xxx
                }
                stack.push(DOT);
                stack.push("0");
            }
            int sz = stack.size();
            for (int i = 0; i < sz; ++i) {
                sb.append(stack.pop());
            }
            forgeValue = Double.parseDouble(sb.toString());
            decimalNum++;
        } while (!isNegative && forgeValue > numericStatisticValue.getMax() || isNegative && forgeValue < numericStatisticValue.getMin());
        double realValue = formRealForgeValue(numericStatisticValue, forgeValue);
        return Double.toString(realValue);
    }

    private static ArrayList<ArrayList<Integer>> generateGroupedBits(ArrayList<Integer> bits, int limit) {
        int groupNum = bits.size() / limit;
        ArrayList<ArrayList<Integer>> groupedBits = new ArrayList<>(groupNum);
        for (int i = 0; i < groupNum; i++) {
            ArrayList<Integer> unit = new ArrayList<>(bits.subList(i * limit, i * limit + limit));
            groupedBits.add(unit);
        }
        return groupedBits;
    }

    // TODO: DANGEROUS!! SHOULD BE CHECKED
    private static double formRealForgeValue(NumericStatisticValue numericStatisticValue, double forgeValue) {
        double diff = forgeValue > 0 ? numericStatisticValue.getMax() - forgeValue : forgeValue - numericStatisticValue.getMin(); // should always >0
        String diffStr = Double.toString(diff);
        String forgeValueStr = Double.toString(forgeValue);
        int scale;
        if (diff >= 1) {
            scale = forgeValue > 1 ? -diffStr.lastIndexOf(DOT) + forgeValueStr.lastIndexOf(DOT) : 0;
        } else {
            if (forgeValue > 1) return forgeValue;
            int diffNonZeroPos = getFirstNonZeroPosInDecimal(diffStr);
            int valNonZeroPos = getFirstNonZeroPosInDecimal(forgeValueStr);
            scale = valNonZeroPos > diffNonZeroPos ? diffNonZeroPos : 0;
        }
        if (scale == 0) return forgeValue;
        BigDecimal bigDecimal = BigDecimal.valueOf(diff);
        double trash = bigDecimal.setScale(scale, RoundingMode.DOWN).doubleValue();
        return forgeValue > 0 ? forgeValue + trash : forgeValue - trash;
    }

    private static int getFirstNonZeroPosInDecimal(String doubleValueString) {
        int dotIndex = doubleValueString.lastIndexOf(DOT);
        String val = doubleValueString.substring(dotIndex + DOT.length());
        byte[] bytes = val.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; ++i) {
            if (bytes[i] != '0') return i;
        }
        return -1;
    }
}
