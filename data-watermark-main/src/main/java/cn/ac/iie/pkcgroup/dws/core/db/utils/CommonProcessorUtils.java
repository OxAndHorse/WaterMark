package cn.ac.iie.pkcgroup.dws.core.db.utils;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.DecoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.EncoderCallable;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.model.PartitionedDataset;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator.WATERMARK_LENGTH;

@Slf4j
public class CommonProcessorUtils {
    static double successThreshold = 0.9;

    public static String detectWatermark(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo, DecoderCallable decoderCallable) {
        int wmLength = WATERMARK_LENGTH;
        int[] zeros = new int[wmLength];
        int[] ones = new int[wmLength];
        int[] candidates = new int[wmLength];

        String secretKey = PartitionUtils.generatePartitionKey(basicTraceInfo);
        PartitionedDataset partitionedDataset = PartitionUtils.divide(datasetWithPK, secretKey);
        Map<Integer, ArrayList<ArrayList<String>>> map = partitionedDataset.getPartitionedDataset();
        int colIndex = basicTraceInfo.getFieldModel().getSelectedField().getFieldIndex();
        for (Map.Entry<Integer, ArrayList<ArrayList<String>>> entry : map.entrySet()) {
            Integer k = entry.getKey();
            ArrayList<ArrayList<String>> v = entry.getValue();
            ArrayList<String> colValues = new ArrayList<>();
            int index = k % wmLength;
            v.forEach(strValues -> colValues.add(strValues.get(colIndex)));
            try {
                if (decoderCallable.isZeroEmbedded(colValues)) {
                    zeros[index]++;
                } else {
                    ones[index]++;
                }
            } catch (WatermarkException e) {
                candidates[index]++;
            }
        }

        StringBuilder wm = new StringBuilder();
        for (int i = 0; i < wmLength; i++) {
            int one = ones[i];
            int zero = zeros[i];
            int err = candidates[i];
            int max = Math.max(Math.max(one, zero), err);
            if (max == err || zero == one) {
                wm.append("?");
            } else if (max == zero) {
                wm.append("0");
            } else {
                wm.append("1");
            }
        }
        return wm.toString();
//        int lastBitOne = recWatermark.lastIndexOf("1");
//        if (lastBitOne % 8 != 0) {
//            log.error("Decoding error: cannot correctly parse the ending pattern.");
//            return null;
//        }
//        return recWatermark.substring(0, lastBitOne);
    }

    public static void encodeAllBits(PartitionedDataset partitionedDataset, ArrayList<Integer> watermark, int colIndex, EncoderCallable encoderCallable) throws WatermarkException {
        Map<Integer, ArrayList<ArrayList<String>>> datasetWithIndex = partitionedDataset.getPartitionedDataset();
        int successCount = 0;

        final int wmLength = watermark.size();
        for (Map.Entry<Integer, ArrayList<ArrayList<String>>> entry : datasetWithIndex.entrySet()) {
            Integer k = entry.getKey();
            ArrayList<ArrayList<String>> v = entry.getValue();
            int index = k % wmLength;
            successCount += encodeSingleBit(v, colIndex, watermark.get(index), encoderCallable);
        }
        int thresholdValue = new Double(partitionedDataset.getTotalCount() * successThreshold).intValue();
        if (successCount < thresholdValue) {
            throw new WatermarkException("Too many failed rows.");
        }
    }

    // Return the count of successfully embedded rows
    private static int encodeSingleBit(ArrayList<ArrayList<String>> partition, int colIndex, int bit, EncoderCallable encoderCallable) {
        int trapCounter = 0;
        for (ArrayList<String> row : partition) {
            String dwString = encoderCallable.generateDWString(row.get(colIndex), bit == 0);
            if (dwString == null) {
                trapCounter++;
                continue;
            }
            row.set(colIndex, dwString);
        }
        return partition.size() - trapCounter;
    }

    // For column expansion
    public static void encodeAllBits(PartitionedDataset partitionedDataset, ArrayList<Integer> watermark, EncoderCallable encoderCallable) throws WatermarkException {
        Map<Integer, ArrayList<ArrayList<String>>> datasetWithIndex = partitionedDataset.getPartitionedDataset();
        int successCount = 0;

        final int wmLength = watermark.size();
        for (Map.Entry<Integer, ArrayList<ArrayList<String>>> entry : datasetWithIndex.entrySet()) {
            Integer k = entry.getKey();
            ArrayList<ArrayList<String>> v = entry.getValue();
            int index = k % wmLength;
            successCount += encodeSingleBit(v, watermark.get(index), encoderCallable);
        }
        int thresholdValue = new Double(partitionedDataset.getTotalCount() * successThreshold).intValue();
        if (successCount < thresholdValue) {
            throw new WatermarkException("Too many failed rows.");
        }
    }

    private static int encodeSingleBit(ArrayList<ArrayList<String>> partition, int bit, EncoderCallable encoderCallable) {
        int trapCounter = 0;
        for (ArrayList<String> row : partition) {
            String dwString = encoderCallable.generateDWString( bit == 0);
            if (dwString == null) {
                trapCounter++;
                continue;
            }
            row.add(dwString);
        }
        return partition.size() - trapCounter;
    }

    public static String getMostPossibleWatermark(ArrayList<String> watermarks) {
        int max = 1;
        String watermark = watermarks.get(0);
        HashMap<String, Integer> hashMap = new HashMap<>();
        for (String dw : watermarks) {
            if (hashMap.containsKey(dw)) {
                int cnt = hashMap.get(dw);
                cnt++;
                if (cnt > max) {
                    watermark = dw;
                    max = cnt;
                }
                hashMap.put(dw, cnt);
            } else {
                hashMap.put(dw, 1);
            }
        }
        return watermark;
    }
}

