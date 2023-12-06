package cn.ac.iie.pkcgroup.dws.core.db;

import cn.ac.iie.pkcgroup.dws.comm.request.db.SelectedField;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.*;
import cn.ac.iie.pkcgroup.dws.core.db.model.*;
import cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.SimpleColExpansionDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.colext.simple.SimpleColExpansionEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.opt.OptimizationDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.opt.OptimizationEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.pad.DecimalPadDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.pad.DecimalPadEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.integer.IntegerDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.integer.IntegerEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.SimpleRowDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.SimpleRowEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.text.punc.PunctuationDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.text.punc.PunctuationEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.text.space.SpaceDecoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.text.space.SpaceEncoder;
import cn.ac.iie.pkcgroup.dws.data.table.TableInfo;
import cn.ac.iie.pkcgroup.dws.data.table.TableInfoWithPK;
import cn.ac.iie.pkcgroup.dws.data.table.TableMap;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static cn.ac.iie.pkcgroup.dws.core.db.Constants.*;

@Slf4j
public class Parser {
    /**
     * 确认字段类型
     */
    // Only parts of data types are supported
    public static String checkDataType(int dataType) {
        if (dataType == Types.DOUBLE || dataType == Types.REAL || dataType == Types.FLOAT || dataType == Types.NUMERIC) {
            return COL_TYPE_DOUBLE;
            // NO INTEGER SUPPORT YET
        } else if (dataType == Types.INTEGER || dataType == Types.BIGINT || dataType == Types.SMALLINT || dataType == Types.TINYINT) {
            return COL_TYPE_INTEGER;
        } else if (dataType == Types.VARCHAR || dataType == Types.LONGVARCHAR) {
            return COL_TYPE_TEXT;
        }
        return COL_NOT_SUPPORTED;
    }

    public static String setInnerDataType(int dataType) {
        if (dataType == Types.DOUBLE || dataType == Types.REAL || dataType == Types.FLOAT || dataType == Types.NUMERIC) {
            return COL_TYPE_DOUBLE;
        } else if (dataType == Types.INTEGER || dataType == Types.BIGINT || dataType == Types.SMALLINT || dataType == Types.TINYINT) {
            return COL_TYPE_INTEGER;
        } else if (dataType == Types.VARCHAR || dataType == Types.LONGVARCHAR) {
            return COL_TYPE_TEXT;
        } else if (dataType == Types.TIME || dataType == Types.TIMESTAMP || dataType == Types.TIME_WITH_TIMEZONE || dataType == Types.TIMESTAMP_WITH_TIMEZONE || dataType == Types.DATE) {
            return COL_TYPE_TIMESTAMP;
        }
        return COL_NOT_USED;
    }

    public static String chooseAlgorithm(FieldModel.FieldUnit fieldUnit) {
        switch (fieldUnit.getFieldType()) {
            case COL_TYPE_DOUBLE:
                return WatermarkAlgorithm.NUMERIC_METHOD_DECIMAL;
            case COL_TYPE_TEXT:
                return WatermarkAlgorithm.TEXT_METHOD_SPACE;
            case COL_TYPE_INTEGER:
                return WatermarkAlgorithm.NUMERIC_METHOD_INTEGER;
            default:
                return null;
        }
    }

    public static void chooseEmbeddingFields(ArrayList<SelectedField> selectedFields, TableInfoWithPK tableInfoWithPK) {
        Map<String, FieldModel.FieldUnit> columnsInfo = tableInfoWithPK.getTableInfo().getColumnsInfo();
        ArrayList<Integer> pkIndex = tableInfoWithPK.getPkIndex();
        for (String colName :
                columnsInfo.keySet()) {
            FieldModel.FieldUnit fieldUnit = columnsInfo.get(colName);
            // 非主键字段
            if (!Objects.equals(fieldUnit.getFieldType(), COL_NOT_SUPPORTED) && !pkIndex.contains(fieldUnit.getFieldIndex())) {
                SelectedField selectedField = new SelectedField();
                selectedField.setFieldName(colName);
                selectedField.setAlgorithm(chooseAlgorithm(fieldUnit));
                selectedFields.add(selectedField);
            }
        }
    }

    /**
     * 根据待嵌入字段的信息，选择encoder
     */
    public static IEncoder parseEncoder(EmbeddingInfo embeddingInfo, TableMap tableMap, SelectedField selectedField) {
        String dbName = embeddingInfo.getDbName();
        String tableName = embeddingInfo.getTableName();
        String identicalKey = StringUtils.generateIdenticalKey(embeddingInfo.getSystemId(), dbName, tableName);
        String embeddingColumnName = selectedField.getFieldName();

        TableInfo tableInfo = tableMap.getInfoMap().get(identicalKey).getTableInfo();
        String dataType = tableInfo.getColumnsInfo().get(embeddingColumnName).getFieldType();
        if (dataType.equals(COL_NOT_SUPPORTED)) {
            log.info("Parse encoder fail.");
            return null;
        }
        // Use specified algorithms
        String embeddingMethod = embeddingInfo.getEmbeddingMethod();
        if (embeddingMethod != null) {
            selectedField.setAlgorithm(embeddingMethod);
            switch (embeddingMethod) {
                case WatermarkAlgorithm.NUMERIC_METHOD_DECIMAL:
                    if (!dataType.equals(COL_TYPE_DOUBLE) && !dataType.equals(COL_TYPE_INTEGER)) return null;
                    return new DecimalPadEncoder();
                case WatermarkAlgorithm.NUMERIC_METHOD_PATTERN_SEARCH:
                    if (!dataType.equals(COL_TYPE_DOUBLE) && !dataType.equals(COL_TYPE_INTEGER)) return null;
                    return new OptimizationEncoder();
                case WatermarkAlgorithm.TEXT_METHOD_SPACE:
                    if (!dataType.equals(COL_TYPE_TEXT)) return null;
                    return new SpaceEncoder();
                case WatermarkAlgorithm.TEXT_METHOD_PUNCTUATION:
                    if (!dataType.equals(COL_TYPE_TEXT)) return null;
                    return new PunctuationEncoder();
                case WatermarkAlgorithm.NUMERIC_METHOD_INTEGER:
                    if (!dataType.equals(COL_TYPE_INTEGER)) return null;
                    return new IntegerEncoder();
                default:
                    return null;
            }
        }
        // Use default algorithms, decided by the data type.
        switch (dataType) {
            case COL_TYPE_DOUBLE:
                // Default use Constants.EmbedDbInfo.NUMERIC_METHOD_DECIMAL for NUMERIC
                selectedField.setAlgorithm(WatermarkAlgorithm.NUMERIC_METHOD_DECIMAL);
                return new DecimalPadEncoder();
            case COL_TYPE_TEXT:
                // Default use Constants.EmbedDbInfo.TEXT_METHOD_SPACE for TEXT
                selectedField.setAlgorithm(WatermarkAlgorithm.TEXT_METHOD_SPACE);
                return new SpaceEncoder();
            case COL_TYPE_INTEGER:
                selectedField.setAlgorithm(WatermarkAlgorithm.NUMERIC_METHOD_INTEGER);
                return new IntegerEncoder();
            default:
                return null;
        }
    }

    public static IDecoder parseDecoder(ExtractInfo extractInfo, TableMap tableMap) {
        String dbName = extractInfo.getDbName();
        String tableName = extractInfo.getSourceTableName();
        String mapKey = StringUtils.generateIdenticalKey(extractInfo.getSystemId(), dbName, tableName);
        String embeddingColumnName = extractInfo.getEmbeddingColumnName();

        TableInfo tableInfo = tableMap.getInfoMap().get(mapKey).getTableInfo();
        String dataType = tableInfo.getColumnsInfo().get(embeddingColumnName).getFieldType();
        if (dataType.equals(COL_NOT_SUPPORTED)) {
            log.info("Parse encoder fail.");
            return null;
        }
        // Use specified algorithms
        String embeddingMethod = extractInfo.getEmbeddingMethod();
        if (embeddingMethod != null) {
            switch (embeddingMethod) {
                case WatermarkAlgorithm.NUMERIC_METHOD_DECIMAL:
                    if (!dataType.equals(COL_TYPE_DOUBLE) && !dataType.equals(COL_TYPE_INTEGER)) return null;
                    return new DecimalPadDecoder();
                case WatermarkAlgorithm.NUMERIC_METHOD_PATTERN_SEARCH:
                    if (!dataType.equals(COL_TYPE_DOUBLE) && !dataType.equals(COL_TYPE_INTEGER)) return null;
                    return new OptimizationDecoder();
                case WatermarkAlgorithm.NUMERIC_METHOD_INTEGER:
                    if (!dataType.equals(COL_TYPE_DOUBLE) && !dataType.equals(COL_TYPE_INTEGER)) return null;
                    return new IntegerDecoder();
                case WatermarkAlgorithm.TEXT_METHOD_SPACE:
                    if (!dataType.equals(COL_TYPE_TEXT)) return null;
                    return new SpaceDecoder();
                case WatermarkAlgorithm.TEXT_METHOD_PUNCTUATION:
                    if (!dataType.equals(COL_TYPE_TEXT)) return null;
                    return new PunctuationDecoder();
                default:
                    return null;
            }
        }
        // Use default algorithms, decided by the data type.
        switch (dataType) {
            case COL_TYPE_DOUBLE:
                // Default use Constants.EmbedDbInfo.NUMERIC_METHOD_DECIMAL for NUMERIC
                return new DecimalPadDecoder();
            case COL_TYPE_TEXT:
                // Default use Constants.EmbedDbInfo.TEXT_METHOD_SPACE for TEXT
                return new SpaceDecoder();
            case COL_TYPE_INTEGER:
                return new IntegerDecoder();
            default:
                return null;
        }
    }

    public static IDecoder parseDecoder(ExtractInfo extractInfo) {
        // Use specified algorithms
        String embeddingMethod = extractInfo.getEmbeddingMethod();
        if (embeddingMethod != null) {
            switch (embeddingMethod) {
                case WatermarkAlgorithm.NUMERIC_METHOD_PATTERN_SEARCH:
                    return new OptimizationDecoder();
                case WatermarkAlgorithm.NUMERIC_METHOD_DECIMAL:
                    return new DecimalPadDecoder();
                case WatermarkAlgorithm.NUMERIC_METHOD_INTEGER:
                    return new IntegerDecoder();
                case WatermarkAlgorithm.TEXT_METHOD_SPACE:
                    return new SpaceDecoder();
                case WatermarkAlgorithm.TEXT_METHOD_PUNCTUATION:
                    return new PunctuationDecoder();
                default:
                    return null;
            }
        }
        log.info("Parse decoder fail.");
        return null;
    }

    public static IRowEncoder parseRowEncoder(RowExpansionConf rowExpansionConf) {
        return new SimpleRowEncoder(rowExpansionConf);
    }

    public static IRowDecoder parseRowDecoder(RowExpansionInfo rowExpansionInfo) {
        switch (rowExpansionInfo.getAlgorithm()) {
            case WatermarkAlgorithm.ROW_SIMPLE_METHOD:
                return new SimpleRowDecoder();
            case "": // other algorithms
            default:
                return null;
        }
    }

    public static IColEncoder parseColExtEncoder(ColExpansionInfo colExpansionInfo) {
        switch (colExpansionInfo.getEncoder()) {
            case WatermarkAlgorithm.NO_IMPLEMENTATION:
                return null;
            case WatermarkAlgorithm.COL_EXT_SIMPLE_METHOD:
            default:
                colExpansionInfo.setEncoder(WatermarkAlgorithm.COL_EXT_SIMPLE_METHOD);
                return new SimpleColExpansionEncoder();
        }
    }

    public static IColDecoder parseColExtDecoder(ColExpansionInfo colExpansionInfo) {
        switch (colExpansionInfo.getEncoder()) {
            case WatermarkAlgorithm.NO_IMPLEMENTATION:
                return null;
            case WatermarkAlgorithm.COL_EXT_SIMPLE_METHOD:
            default:
                colExpansionInfo.setEncoder(WatermarkAlgorithm.COL_EXT_SIMPLE_METHOD);
                return new SimpleColExpansionDecoder();
        }
    }
}
