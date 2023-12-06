package cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple;

import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IRowDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.*;
import cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.text.InlineRowSpaceDecoder;
import cn.ac.iie.pkcgroup.dws.data.table.TableInfoWithPK;

import java.util.ArrayList;
import java.util.HashMap;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator.WATERMARK_LENGTH;
import static cn.ac.iie.pkcgroup.dws.core.db.processor.row.Constants.NO_DW;
import static cn.ac.iie.pkcgroup.dws.core.db.utils.CommonProcessorUtils.getMostPossibleWatermark;

public class SimpleRowDecoder implements IRowDecoder {
    /**
     * @param dataset dataset
     * @param basicTraceInfo NOT USED
     * @return extracted watermark
     */
    @Override
    public String decode(Dataset dataset, BasicTraceInfo basicTraceInfo) {
        String extractedWatermark;
        ArrayList<ArrayList<String>> rowDataList = new ArrayList<>(dataset.getRawDataset());
        int limit = basicTraceInfo.getRowExpansionInfo().getMaxCapacity();
        int maxCapacity = limit == -1 ? WATERMARK_LENGTH : limit;
        // all fields are processed as TEXT
        ArrayList<String> extractedWatermarks = new ArrayList<>();
        for (ArrayList<String> rowData : rowDataList) {
            int detectedBits = 0;
            StringBuilder sb = new StringBuilder();
            for (String rowDatum : rowData) {
                String r = InlineRowSpaceDecoder.decode(rowDatum);
                if (r != null && !r.equals(NO_DW)) {
                    int usedBits = Math.min(maxCapacity, r.length()); // BAD checking
                    detectedBits += usedBits;
                    sb.append(r, 0, usedBits);
                }
            }
            if (detectedBits > 0) {
                if (detectedBits < WATERMARK_LENGTH) {
                    for (int i = 0; i < WATERMARK_LENGTH - detectedBits; ++i)
                        sb.append("?"); // use ? to fulfill extracted watermark.
                } else if (detectedBits > WATERMARK_LENGTH) {
                    sb.delete(WATERMARK_LENGTH, sb.length());
                }
                extractedWatermarks.add(sb.toString());
            }
        }
        if (extractedWatermarks.isEmpty()) return null;
        extractedWatermark = getMostPossibleWatermark(extractedWatermarks);
        return extractedWatermark;
    }


}
