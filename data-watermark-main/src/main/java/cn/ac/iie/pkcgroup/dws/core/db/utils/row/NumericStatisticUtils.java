package cn.ac.iie.pkcgroup.dws.core.db.utils.row;

import lombok.Data;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class NumericStatisticUtils {
    public static String DOT = ".";
    @Data
    public static class NumericStatisticValue {
        Double ave;
        Double max;
        Double min;
        int maxDecimalNum;
        int aveDecimalNum;
    }

    public static NumericStatisticValue getAllStatisticValue(ArrayList<Double> data) {
        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
        data.forEach(descriptiveStatistics::addValue);
        NumericStatisticValue numericStatisticValue = new NumericStatisticValue();
        // get max decimal number
        AtomicInteger maxDecimalNum = new AtomicInteger();
        AtomicInteger sumDecimalNum = new AtomicInteger(0);
        int dotLen = DOT.length();
        data.forEach(v -> {
            int sz = String.valueOf(v).length() - String.valueOf(v).lastIndexOf(DOT) - dotLen;
            maxDecimalNum.set(Math.max(maxDecimalNum.get(), sz));
            sumDecimalNum.set(sumDecimalNum.get() + sz);
        });

        numericStatisticValue.setMaxDecimalNum(maxDecimalNum.get());
        numericStatisticValue.setAveDecimalNum((int) Math.round(sumDecimalNum.get() * 1.0 / data.size()));
        numericStatisticValue.setAve(descriptiveStatistics.getMean());
        numericStatisticValue.setMax(descriptiveStatistics.getMax());
        numericStatisticValue.setMin(descriptiveStatistics.getMin());
        return numericStatisticValue;
    }

    public static int calcDoubleCapacity(String value, int decimalNum) {
        int nonZeroPos = 0;
        int vDecimalNum = value.length() - value.lastIndexOf(DOT) - DOT.length();
        int extendedDecimalNum = decimalNum > vDecimalNum ? decimalNum - vDecimalNum : 0;
        String processValue = value;
        if (processValue.startsWith("-")) {
            nonZeroPos++;
            processValue = value.substring(1);
        }
        if (processValue.startsWith("0")) { // 0.xxxx;
            nonZeroPos += 2;
            processValue = processValue.substring(2); // skip 0.
            // remove all zero
            for (int i = 0; i < value.length(); ++i) {
                if (processValue.charAt(i) != '0')
                    nonZeroPos += i;
            }
        }
        int res = value.charAt(nonZeroPos) == '9' ? value.length() - nonZeroPos + extendedDecimalNum : value.length() - nonZeroPos - 1 + extendedDecimalNum;
        return Math.abs(Double.parseDouble(value)) > 1 ? res - DOT.length() : res;
    }

    public static ArrayList<String> forgeRelatedIntegerValue(NumericStatisticValue numericStatisticValue, int num) {
        long maxValue = Math.round(numericStatisticValue.getMax());
        long minValue = Math.round(numericStatisticValue.getMin());
        SecureRandom secureRandom = new SecureRandom(Long.toString(new Date().getTime()).getBytes(StandardCharsets.UTF_8));
        ArrayList<Long> rawData = (ArrayList<Long>) secureRandom.longs(num, minValue, maxValue).boxed().collect(Collectors.toList());
        ArrayList<String> res = new ArrayList<>(rawData.size());
        for (Long l : rawData)
            res.add(Long.toString(l));
        return res;
    }

    public static ArrayList<String> forgeAutoIncrementIntegerValue(NumericStatisticValue numericStatisticValue, int num) {
        long cnt = Math.round(numericStatisticValue.getMax());
        ArrayList<String> res = new ArrayList<>(num);
        for (int i = 0; i < num; ++i) {
            cnt++;
            res.add(Long.toString(cnt));
        }
        return res;
    }

    public static ArrayList<String> forgeRelatedDoubleValue(NumericStatisticValue numericStatisticValue, int num) {
        double maxValue = numericStatisticValue.getMax();
        double minValue = numericStatisticValue.getMin();
        SecureRandom secureRandom = new SecureRandom(Long.toString(new Date().getTime()).getBytes(StandardCharsets.UTF_8));
        ArrayList<Double> rawData = (ArrayList<Double>) secureRandom.doubles(num, minValue, maxValue).boxed().collect(Collectors.toList());
        ArrayList<String> res = new ArrayList<>(rawData.size());
        for (Double l : rawData) {
            BigDecimal bigDecimal = BigDecimal.valueOf(l);
            double v = bigDecimal.setScale(numericStatisticValue.getAveDecimalNum(), RoundingMode.DOWN).doubleValue();
            res.add(Double.toString(v));
        }
        return res;
    }
}
