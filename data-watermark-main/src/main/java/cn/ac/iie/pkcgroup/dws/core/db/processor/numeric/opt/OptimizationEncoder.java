package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.opt;

import cn.ac.iie.pkcgroup.dws.config.algorithms.db.PatternSearchConf;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.utils.PartitionUtils;
import cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator;
import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.model.*;
import cn.ac.iie.pkcgroup.dws.data.dao.NumericOptAuxInfoRepository;
import cn.ac.iie.pkcgroup.dws.data.dao.entity.NumericOptAuxInfoEntity;
import cn.ac.iie.pkcgroup.dws.utils.SpringContextUtils;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;

import static cn.ac.iie.pkcgroup.dws.core.db.Constants.PRIMARY_KEY_DIVIDER;

/**
 * ***结果有问题，勿用！***
 * 基于最优化算法的水印编码器，目前仅支持浮点数类型
 */
@EqualsAndHashCode()
@Data
@Slf4j
public class OptimizationEncoder implements IEncoder {
    private OptimizationAlgorithm optimizationAlgorithm;
    private NumericOptAuxInfoRepository numericOptAuxInfoRepository;

    public OptimizationEncoder() {
        optimizationAlgorithm = new OptimizationAlgorithm();
        numericOptAuxInfoRepository = (NumericOptAuxInfoRepository) SpringContextUtils.getContext().getBean("numericOptAuxInfoRepository");
    }

    @Override
    public void encode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) throws WatermarkException {
        OptimizationTraceInfo optimizationTraceInfo = OptimizationTraceInfo.generateFromBasic(basicTraceInfo);
        encode(datasetWithPK, optimizationTraceInfo, null);
    }

    /**
     * TODO: support for specified parameters.
     */
    public void encode(DatasetWithPK datasetWithPK, OptimizationTraceInfo optimizationTraceInfo, PatternSearchConf patternSearchConf) throws WatermarkException {
        // Use systemId, dbName, tableName, fieldName to generate secret key.
        // Obviously, it is NOT SAFE. TODO: CHANGE the key generation.
        String secretKey = PartitionUtils.generatePartitionKey(optimizationTraceInfo);
        double secretCode = SecretKeyGenerator.generateSecretCode(StringUtils.generateIdenticalKey(optimizationTraceInfo.getDbName(), optimizationTraceInfo.getTableName()));
        optimizationTraceInfo.setSecretCode(secretCode);
        optimizationTraceInfo.setSecretKey(secretKey);

        //对datasetWithPK进行划分
        int partitionCount = PartitionUtils.M;
        optimizationTraceInfo.setPartitionCount(partitionCount);
        PartitionedDataset partitionedDataset = PartitionUtils.divide(datasetWithPK, secretKey);
        //生成水印
        Watermark waterMark = WaterMarkGenerator.generateWatermarkFromMessage(optimizationTraceInfo.getEmbeddingMessage());
        //嵌入水印所有位
        double threshold = encodeAllBits(partitionedDataset, waterMark.getBinary(), optimizationTraceInfo, patternSearchConf);
        // 补充完秘钥信息
        optimizationTraceInfo.setThreshold(threshold);
        optimizationTraceInfo.setWatermark(waterMark);

        //更新数据
        Map<String, ArrayList<String>> ds = datasetWithPK.getDataset();
        ds.clear();
        //把partitionedDataset更新回datasetWithPK，map的key为主键值
        for (ArrayList<ArrayList<String>> rowSet : partitionedDataset.getPartitionedDataset().values()) {
            for (ArrayList<String> row : rowSet) {
                ds.put(generateIndexWithPK(optimizationTraceInfo, row), row);
            }
        }
        // Record to database.
        recordTraceInfo(optimizationTraceInfo);
    }

    /**
     * @param partitionedDataset 整个划分好的数据集
     * @param watermark          要嵌入的水印串
     * @apiNote 对划分好的数据集嵌入水印，直接修改划分里的数据集
     */
    private double encodeAllBits(PartitionedDataset partitionedDataset, ArrayList<Integer> watermark, OptimizationTraceInfo optimizationTraceInfo, PatternSearchConf patternSearchConf) {
        log.info("Start to encode all bits.");
        Map<Integer, ArrayList<ArrayList<String>>> datasetWithIndex = partitionedDataset.getPartitionedDataset();
        final int wmLength = watermark.size();
        int colIndex = optimizationTraceInfo.getFieldModel().getSelectedField().getFieldIndex();

        ArrayList<Double> all = new ArrayList<>();
        datasetWithIndex.forEach((k, v) -> {
            for (ArrayList<String> row : v) {
                // 只取一列数据
                double value = Double.parseDouble(row.get(colIndex));
                all.add(value);
            }
        });
        all.sort(Double::compareTo);

        // 按百分比设置
        double secretCode = optimizationTraceInfo.getSecretCode();
        int start = ((int) (20 + secretCode * 10)) * all.size() / 100;
        int end = ((int) (80 - secretCode * 10)) * all.size() / 100;

        // sigmoid隐藏函数参数
        double oref = 0d;
        for (int i = start; i < end; i++) {
            oref += all.get(i) / (end - start);
        }

        double finalOref = oref;
        ArrayList<Double> minList = new ArrayList<>();
        ArrayList<Double> maxList = new ArrayList<>();
        datasetWithIndex.forEach((k, v) -> {
            int index = k % wmLength;
            encodeSingleBit(v, finalOref, watermark.get(index), colIndex, minList, maxList, patternSearchConf);
        });
        double threshold = optimizationAlgorithm.calcOptimizedThreshold(minList, maxList);
        log.debug("阈值为: {}", threshold);
        // 打印maxList和minList
        log.debug("maxList: {}", maxList);
        log.debug("minList: {}", minList);

        return threshold;
    }

    /**
     * @param partition 一个划分
     * @param bit       水印对应的bit位
     * @apiNote 对水印的一个bit嵌入一个划分当中，直接对划分进行修改
     */
    private void encodeSingleBit(ArrayList<ArrayList<String>> partition, double oref, int bit, int colIndex, ArrayList<Double> minList, ArrayList<Double> maxList, PatternSearchConf patternSearchConf) {
        //System.out.printf("正在对第%d个字段嵌入水印的第%d位: %d%n", COL_INDEX +1, bitIndex, bit);
        ArrayList<Double> colValues = new ArrayList<>();

        for (ArrayList<String> row : partition) {
            // 只取一列数据
            double value = Double.parseDouble(row.get(colIndex));
            colValues.add(value);
        }
        PatternSearch.SearchResult searchResult = new PatternSearch.SearchResult();
        // 该bit是0则将hiding函数最小化，是1则最大化
        switch (bit) {
            case 0:
                searchResult = optimizationAlgorithm.getPatternSearch().minimizeByHidingFunction(colValues,
                        oref, patternSearchConf);
                minList.add(searchResult.getResult());
                break;
            case 1:
                searchResult = optimizationAlgorithm.getPatternSearch().maximizeByHidingFunction(colValues,
                        oref, patternSearchConf);
                maxList.add(searchResult.getResult());
                break;
            default:
                log.error("水印出错! ");
                break;
        }

        ArrayList<Double> modifiedCol = searchResult.getInitState();

        // always double
        String placeholder = optimizationAlgorithm.getPatternSearch().formatOutput(patternSearchConf, true);
        // 写回partition
        int rowIndex = 0;
        String resultStr;
        for (ArrayList<String> row : partition) {
            resultStr = String.format(placeholder, modifiedCol.get(rowIndex));
            row.set(colIndex, resultStr);
            rowIndex++;
        }
    }

    @Override
    public String toString() {
        return "Optimization based Encoder";
    }

    private String generateIndexWithPK(BasicTraceInfo basicTraceInfo, ArrayList<String> rowData) {
        FieldModel fieldModel = basicTraceInfo.getFieldModel();
        StringBuilder pk = new StringBuilder();
        for (FieldModel.FieldUnit fieldUnit :
                fieldModel.getPrimaryKeys()) {
            int pkIndex = fieldUnit.getFieldIndex();
            String data = rowData.get(pkIndex);
            pk.append(data).append(PRIMARY_KEY_DIVIDER);
        }
        pk.deleteCharAt(pk.length() - 1);
        return pk.toString();
    }


    private void recordTraceInfo(OptimizationTraceInfo optimizationTraceInfo) {
        NumericOptAuxInfoEntity numericOptAuxInfoEntity = new NumericOptAuxInfoEntity();
        numericOptAuxInfoEntity.setSystemId(optimizationTraceInfo.getSystemId());
        numericOptAuxInfoEntity.setDbName(optimizationTraceInfo.getDbName());
        numericOptAuxInfoEntity.setTableName(optimizationTraceInfo.getTableName());
        numericOptAuxInfoEntity.setEmbeddedMsg(optimizationTraceInfo.getEmbeddingMessage());
        numericOptAuxInfoEntity.setPartitionCount(optimizationTraceInfo.getPartitionCount());
        numericOptAuxInfoEntity.setSecretKey(optimizationTraceInfo.getSecretKey());
        numericOptAuxInfoEntity.setSecretCode(optimizationTraceInfo.getSecretCode());
        numericOptAuxInfoEntity.setThreshold(optimizationTraceInfo.getThreshold());
        numericOptAuxInfoEntity.setWatermark(optimizationTraceInfo.getWatermark().toString());
        numericOptAuxInfoEntity.setWmField(optimizationTraceInfo.getFieldModel().getSelectedField().getFieldName());
        numericOptAuxInfoRepository.saveAndFlush(numericOptAuxInfoEntity);
    }

}

