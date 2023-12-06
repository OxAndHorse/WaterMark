package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.opt;

import cn.ac.iie.pkcgroup.dws.core.db.utils.PartitionUtils;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.model.PartitionedDataset;
import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.data.dao.NumericOptAuxInfoRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.NumericOptAuxInfoEntity;
import cn.ac.iie.pkcgroup.dws.utils.SpringContextUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator.WATERMARK_LENGTH;

/**
 * 基于最优化算法的水印解码器
 */
@Data
@Slf4j
public class OptimizationDecoder implements IDecoder {
    private OptimizationAlgorithm optimizationAlgorithm;
    private NumericOptAuxInfoRepository numericOptAuxInfoRepository;
    private int watermarkLength = WATERMARK_LENGTH;

    public OptimizationDecoder() {
        optimizationAlgorithm = new OptimizationAlgorithm();
        numericOptAuxInfoRepository = (NumericOptAuxInfoRepository) SpringContextUtils.getContext().getBean("numericOptAuxInfoRepository");
    }

    @Override
    public String decode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) {
        log.debug("解码使用的元组数有 {}", datasetWithPK.getDataset().size());
        OptimizationTraceInfo optimizationTraceInfo = OptimizationTraceInfo.generateFromBasic(basicTraceInfo);
        NumericOptAuxInfoEntity numericOptAuxInfoEntity = numericOptAuxInfoRepository.findFirstBySystemIdAndDbNameAndTableNameAndWmField(basicTraceInfo.getSystemId(), basicTraceInfo.getDbName(),
                basicTraceInfo.getTableName(), basicTraceInfo.getFieldModel().getSelectedField().getFieldName());
        if (numericOptAuxInfoEntity != null) {
            optimizationTraceInfo.setSecretKey(numericOptAuxInfoEntity.getSecretKey());
            optimizationTraceInfo.setSecretCode(numericOptAuxInfoEntity.getSecretCode());
            optimizationTraceInfo.setThreshold(numericOptAuxInfoEntity.getThreshold());
        } else {
            log.info("No trace information can be found from the db.");
            return null;
        }
        String decodedWatermark = detectWatermark(datasetWithPK, optimizationTraceInfo);
        log.debug("解码出来的水印为: {}", decodedWatermark);
        return decodedWatermark;
    }

    private String detectWatermark(DatasetWithPK datasetWithPK, OptimizationTraceInfo optimizationTraceInfo) {
        int[] ones = new int[watermarkLength];
        int[] zeros = new int[watermarkLength];
        PartitionedDataset partitionedDataset = PartitionUtils.divide(datasetWithPK, optimizationTraceInfo.getSecretKey());
        Map<Integer, ArrayList<ArrayList<String>>> map = partitionedDataset.getPartitionedDataset();
        int colIndex = optimizationTraceInfo.getFieldModel().getSelectedField().getFieldIndex();
        ArrayList<Double> all = new ArrayList<>();
        map.forEach((k, v) -> {
            for (ArrayList<String> row : v) {
                // 只取一列数据
                double value = Double.parseDouble(row.get(colIndex));
                all.add(value);
            }
        });
        all.sort(Double::compareTo);
        // 使用secretKey进行映射
        double secretCode = optimizationTraceInfo.getSecretCode();
        double threshold = optimizationTraceInfo.getThreshold();
        int start = ((int) (20 + secretCode * 10)) * all.size() / 100;
        int end = ((int) (80 - secretCode * 10)) * all.size() / 100;
        double oref = 0d;

        for (int i = start; i < end; i++) {
            oref += all.get(i) / (end - start);
        }

        double finalOref = oref;
        map.forEach((k, v) -> {
            ArrayList<Double> colValues = new ArrayList<>();
            int index = k % watermarkLength;
            v.forEach(strValues -> colValues.add(Double.parseDouble(strValues.get(colIndex))));
            double hidingValue = optimizationAlgorithm.getOHidingValue(colValues, finalOref);
            if (hidingValue > threshold) {
                ones[index]++;
            } else {
                zeros[index]++;
            }
        });

        //据ones和zeros生成水印
        StringBuilder wm = new StringBuilder();
        for (int i = 0; i < watermarkLength; i++) {
            if (ones[i] > zeros[i]) {
                log.debug("第{}位有{}个0，{}个1，解得1", i, zeros[i], ones[i]);
                wm.append("1");
            } else if (ones[i] < zeros[i]) {
                log.debug("第{}位有{}个0，{}个1，解得0", i, zeros[i], ones[i]);
                wm.append("0");
            } else {
                log.debug("第{}位有{}个0，{}个1，解得x", i, zeros[i], ones[i]);
                wm.append("x");
            }
        }
        return wm.toString();
    }
}
