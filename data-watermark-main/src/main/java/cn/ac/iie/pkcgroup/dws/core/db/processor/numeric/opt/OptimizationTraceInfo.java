package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.opt;

import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.FieldModel;
import cn.ac.iie.pkcgroup.dws.core.db.model.Watermark;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class OptimizationTraceInfo extends BasicTraceInfo {
    private double secretCode;
    private String secretKey;
    private double threshold;
    private int partitionCount;

    public static OptimizationTraceInfo generateFromBasic(BasicTraceInfo basicTraceInfo) {
        return OptimizationTraceInfo.builder()
                .systemId(basicTraceInfo.getSystemId())
                .watermark(basicTraceInfo.getWatermark())
                .dbName(basicTraceInfo.getDbName())
                .tableName(basicTraceInfo.getTableName())
                .embeddingMessage(basicTraceInfo.getEmbeddingMessage())
                .fieldModel(basicTraceInfo.getFieldModel())
                .build();
    }
}
