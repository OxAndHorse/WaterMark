package cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.db.Constants;
import cn.ac.iie.pkcgroup.dws.core.db.Parser;
import cn.ac.iie.pkcgroup.dws.core.db.interfaces.IRowEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.model.*;
import cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.numeric.InlineRowPadEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.processor.row.simple.text.InlineRowSpaceEncoder;
import cn.ac.iie.pkcgroup.dws.core.db.utils.row.NumericStatisticUtils;
import cn.ac.iie.pkcgroup.dws.core.db.utils.row.NumericStatisticUtils.NumericStatisticValue;
import cn.ac.iie.pkcgroup.dws.core.db.utils.row.TextStatisticUtils;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.criteria.CriteriaBuilder;
import java.security.SecureRandom;
import java.util.*;

import static cn.ac.iie.pkcgroup.dws.core.db.processor.WaterMarkGenerator.WATERMARK_LENGTH;
import static cn.ac.iie.pkcgroup.dws.utils.StringUtils.getRowPrimaryKeyValue;

@Slf4j
@NoArgsConstructor
public class SimpleRowEncoder implements IRowEncoder {
    private int limitPerField = -1; // -1 for unlimited, >0 for max watermark bits in one field
    private double rowNumThreshold = 0.01;
    private final int capacityPerPosition = 3; // 3bit for 1-8, used for double type.

    public SimpleRowEncoder(RowExpansionConf rowExpansionConf) {
        limitPerField = rowExpansionConf.getRowCapacityLimit();
        rowNumThreshold = rowExpansionConf.getForgeRowNumThreshold();
    }

    @Data
    @Builder
    public static class AnalyzedField {
        int capacity; //
        Object analyzedValue; // used for forging
    }

    @Override
    public void encode(DatasetWithPK datasetWithPK, BasicTraceInfo basicTraceInfo) throws WatermarkException {
        ArrayList<ArrayList<String>> rowDataList = new ArrayList<>(datasetWithPK.getDataset().values());
        ArrayList<FieldModel.FieldUnit> metaData = datasetWithPK.getMetaData();
        ArrayList<AnalyzedField> analyzedFields = divideFieldCapacity(rowDataList, metaData);
        Watermark watermark = basicTraceInfo.getWatermark();

        int forgeSize = (int) Math.round(rowNumThreshold * rowDataList.size());
        if (forgeSize == 0) throw new WatermarkException("Lacking enough data to apply row encoder.");
        ArrayList<ArrayList<String>> forgeDataAsColumn = getForgeDataAsColumn(rowDataList, metaData, analyzedFields, forgeSize);
        ArrayList<ArrayList<String>> forgeDataAsRow = changeFormColumnToRow(forgeDataAsColumn);
        forgeDataAsColumn.clear();

        for (ArrayList<String> forgeRowData : forgeDataAsRow) {
            ArrayList<Integer> capacityList = getRandomTargetCapacity(analyzedFields, metaData);
            int head = 0;
            for (int j = 0; j < metaData.size(); ++j) {
                if (metaData.get(j).isPrimaryKey()) continue;
                AnalyzedField analyzedField = analyzedFields.get(j);
                String type = Parser.setInnerDataType(metaData.get(j).getFieldTypeCode());
                ArrayList<Integer> bits = new ArrayList<>(watermark.getBinary().subList(head, head + capacityList.get(j)));
                head += capacityList.get(j);
                switch (type) {
                    case Constants.COL_TYPE_TEXT:
                        forgeRowData.set(j, InlineRowSpaceEncoder.encode(forgeRowData.get(j), bits));
                        break;
                    case Constants.COL_TYPE_DOUBLE:
                        forgeRowData.set(j, InlineRowPadEncoder.encode(forgeRowData.get(j), analyzedField, bits, capacityPerPosition));
                        break;
                    case Constants.COL_TYPE_INTEGER:
                    default:
                        break;
                }
            }
        }
        // insert forged data into original data list
        ArrayList<Integer> pkIndex = getPrimaryKeyIndex(metaData);
        Map<String, ArrayList<String>> dsMap = datasetWithPK.getDataset();
        forgeDataAsRow.forEach(rowData -> {
            String rowPK = getRowPrimaryKeyValue(pkIndex, rowData);
            dsMap.put(rowPK, rowData);
        });
        // when pk is auto increment field & using row expansion, should be sorted by other fields.
        int sortIndex = chooseProperSortIndex(metaData);
        datasetWithPK.setSortedIndex(sortIndex);
    }

    private ArrayList<Integer> getPrimaryKeyIndex(ArrayList<FieldModel.FieldUnit> metaData) {
        ArrayList<Integer> pkIndex = new ArrayList<>();
        int index = 0;
        for (FieldModel.FieldUnit fieldUnit : metaData) {
            if (fieldUnit.isPrimaryKey()) pkIndex.add(index);
            index++;
        }
        return pkIndex; // If no pk, use 1st column
    }

    private int chooseProperSortIndex(ArrayList<FieldModel.FieldUnit> metaData) {
        int sortIndex = 0;
        LinkedHashMap<Integer, Integer> candidateIndex = new LinkedHashMap<>();
        int cnt = 0;
        boolean success = false;
        for (FieldModel.FieldUnit fieldUnit : metaData) {
            String type = Parser.setInnerDataType(fieldUnit.getFieldTypeCode());
            boolean isNumeric = type.equals(Constants.COL_TYPE_DOUBLE) || type.equals(Constants.COL_TYPE_INTEGER);
            boolean isText = type.equals(Constants.COL_TYPE_TEXT);
            if (fieldUnit.isPrimaryKey() && !fieldUnit.isAutoIncrement()) {
                sortIndex = cnt;
                success = true;
                break;
            } else if (!fieldUnit.isPrimaryKey() && !fieldUnit.isAutoIncrement() && isNumeric) {
                candidateIndex.put(cnt, 1);
            } else if (!fieldUnit.isPrimaryKey() && isText) {
                candidateIndex.put(cnt, 2);
            }
            cnt++;
        }
        // find other non-primary key field
        if (!success && !candidateIndex.isEmpty()) {
            List<Map.Entry<Integer, Integer>> entryList = new ArrayList<>(
                    candidateIndex.entrySet());
            entryList.sort(Map.Entry.comparingByValue());
            sortIndex = entryList.get(0).getKey(); // find the highest priority one
        }
        return sortIndex;
    }

    private boolean isSinglePrimaryKey(ArrayList<FieldModel.FieldUnit> metaData) {
        int pkCount = 0;
        for (FieldModel.FieldUnit fieldUnit : metaData) {
            if (fieldUnit.isPrimaryKey()) pkCount++;
        }
        return pkCount == 1;
    }

    private ArrayList<ArrayList<String>> getForgeDataAsColumn(ArrayList<ArrayList<String>> rowDataList,
                                                              ArrayList<FieldModel.FieldUnit> metaData,
                                                              ArrayList<AnalyzedField> analyzedFields,
                                                              int forgeSize) throws WatermarkException {
        ArrayList<ArrayList<String>> forgeDataAsColumn = new ArrayList<>();
        for (int j = 0; j < metaData.size(); ++j) {
            AnalyzedField analyzedField = analyzedFields.get(j);
            String type = Parser.setInnerDataType(metaData.get(j).getFieldTypeCode());
            switch (type) {
                case Constants.COL_TYPE_TEXT:
                    ArrayList<String> material = new ArrayList<>(rowDataList.size());
                    for (ArrayList<String> rowData : rowDataList)
                        material.add(rowData.get(j));
                    ArrayList<String> forge = TextStatisticUtils.forgeRelatedText(material, forgeSize, isSinglePrimaryKey(metaData) && metaData.get(j).isPrimaryKey());
                    if (forge == null) throw new WatermarkException("Do not support for such single primary key.");
                    forgeDataAsColumn.add(forge);
                    break;
                case Constants.COL_TYPE_DOUBLE:
                    forgeDataAsColumn.add(NumericStatisticUtils.forgeRelatedDoubleValue((NumericStatisticValue) analyzedField.getAnalyzedValue(), forgeSize));
                    break;
                case Constants.COL_TYPE_INTEGER:
                    if (metaData.get(j).isAutoIncrement()) forgeDataAsColumn.add(NumericStatisticUtils.forgeAutoIncrementIntegerValue((NumericStatisticValue) analyzedField.getAnalyzedValue(), forgeSize));
                    else forgeDataAsColumn.add(NumericStatisticUtils.forgeRelatedIntegerValue((NumericStatisticValue) analyzedField.getAnalyzedValue(), forgeSize));
                    break;
                default:
                    break;
            }
        }
        return forgeDataAsColumn;
    }

    public ArrayList<AnalyzedField> divideFieldCapacity(ArrayList<ArrayList<String>> rowDataList, ArrayList<FieldModel.FieldUnit> metaData) throws WatermarkException {
        ArrayList<AnalyzedField> analyzedFields = new ArrayList<>();
        int totalCapacity = 0;
        int subCapacity;
        int flagOccupy = 1;
        int maxCapacity = limitPerField > 0 ? Math.min(limitPerField, WATERMARK_LENGTH) : WATERMARK_LENGTH; // cannot exceed the max capacity
        String err;
        for (int i = 0; i < metaData.size(); ++i) {
            FieldModel.FieldUnit fieldUnit = metaData.get(i);
            // use raw field type
            String type = Parser.setInnerDataType(fieldUnit.getFieldTypeCode());
            AnalyzedField analyzedField = AnalyzedField.builder().build();
            NumericStatisticValue numericStatisticValue;
            switch (type) {
                case Constants.COL_TYPE_TEXT:
                    double aveLen = sumTextFieldLength(rowDataList, i);
                    if (aveLen > fieldUnit.getFieldSize()) {
                        err = String.format("Internal error! Mismatched data length for field [%s]", fieldUnit.getFieldName());
                        log.error(err);
                        throw new WatermarkException(err); // Internal error.
                    }
                    int forgeFieldLength = (int) Math.round(aveLen);
                    if (forgeFieldLength == 0) subCapacity = 0; // empty field data
                    else subCapacity = Math.min(fieldUnit.getFieldSize() - forgeFieldLength - flagOccupy, maxCapacity);
                    break;
                case Constants.COL_TYPE_DOUBLE:
                    // TODO: Not find a suitable approach yet, always set subCapacity to 0
                    numericStatisticValue = parseDoubleField(rowDataList, i);
                    if (numericStatisticValue == null) {
                        err = String.format("No valid single/double precision value for field [%s]!", fieldUnit.getFieldName());
                        log.error(err);
                        throw new WatermarkException(err); // Internal error.
                    }
//                        subCapacity = Math.min(checkDoubleCapacity(numericStatisticValue), maxCapacity);
                    analyzedField.setAnalyzedValue(numericStatisticValue);
                    subCapacity = 0;
                    break;
                case Constants.COL_TYPE_INTEGER:
                    // TODO: Not support yet, only for statistic usage
                    numericStatisticValue = parseIntegerField(rowDataList, i);
                    if (numericStatisticValue == null) {
                        err = String.format("No valid integer value for field [%s]!", fieldUnit.getFieldName());
                        log.error(err);
                        throw new WatermarkException(err); // Internal error.
                    }
                    analyzedField.setAnalyzedValue(numericStatisticValue);
                default:
                    subCapacity = 0;
                    break;
            }
            if (metaData.get(i).isPrimaryKey()) subCapacity = 0;
            if (subCapacity > 0) totalCapacity += subCapacity;
            analyzedField.setCapacity(subCapacity);
            analyzedFields.add(analyzedField);
        }
        if (totalCapacity < WATERMARK_LENGTH) {
            err = "No capacity left for row encoding.";
            log.error(err);
            throw new WatermarkException(err);
        }
        return analyzedFields;
    }

    private double sumTextFieldLength(ArrayList<ArrayList<String>> data, int fieldIndex) {
        int sumLen = 0;
        for (ArrayList<String> row : data) {
            String value = row.get(fieldIndex);
            sumLen += value == null ? 0 : value.length();
        }
        return sumLen * 1.0 / data.size();
    }

    private NumericStatisticValue parseDoubleField(ArrayList<ArrayList<String>> data, int fieldIndex) {
        int validDataNum = 0;
        ArrayList<Double> values = new ArrayList<>(data.size());
        for (ArrayList<String> row : data) {
            try {
                double v = Double.parseDouble(row.get(fieldIndex));
                values.add(v);
                validDataNum++;
            } catch (NumberFormatException e) {
                // do nothing;
            }
        }
        if (validDataNum == 0) return null;
        return NumericStatisticUtils.getAllStatisticValue(values);
    }

    private NumericStatisticValue parseIntegerField(ArrayList<ArrayList<String>> data, int fieldIndex) {
        int validDataNum = 0;
        ArrayList<Double> values = new ArrayList<>(data.size());
        for (ArrayList<String> row : data) {
            try {
                long v = Long.parseLong(row.get(fieldIndex));
                values.add(v * 1.0);
                validDataNum++;
            } catch (NumberFormatException e) {
                // do nothing;
            }
        }
        if (validDataNum == 0) return null;
        return NumericStatisticUtils.getAllStatisticValue(values);
    }

    /**
     * Find a minimum capacity according to statistic values of this field.
     *
     * @param numericStatisticValue statistic values.
     * @return watermark capacity for this field.
     */
    private int checkDoubleCapacity(NumericStatisticValue numericStatisticValue) {
        double ave = numericStatisticValue.getAve();
        int decimalNum = numericStatisticValue.getAveDecimalNum();
        return NumericStatisticUtils.calcDoubleCapacity(Double.toString(ave), decimalNum);
    }

    // TODO: should be improved
    private ArrayList<Integer> getRandomTargetCapacity(ArrayList<AnalyzedField> analyzedFields, ArrayList<FieldModel.FieldUnit> metaData) {
        ArrayList<Integer> randomCapacityList = new ArrayList<>(metaData.size());
        int resCapacity;
        do {
            resCapacity = WATERMARK_LENGTH;
            SecureRandom secureRandom = new SecureRandom();
            randomCapacityList.clear();
            for (int i = 0; i < analyzedFields.size(); ++i) {
                int currentCapacity = analyzedFields.get(i).getCapacity();
                if (currentCapacity == 0) {
                    randomCapacityList.add(0);
                    continue;
                }
                String type = Parser.setInnerDataType(metaData.get(i).getFieldTypeCode());
                currentCapacity = type.equals(Constants.COL_TYPE_DOUBLE)
                        ? (secureRandom.nextInt(currentCapacity / capacityPerPosition) + 1) * capacityPerPosition // divided by capacityPerPosition
                        : secureRandom.nextInt(currentCapacity) + 1;
                int usedCapacity = type.equals(Constants.COL_TYPE_DOUBLE) ? Math.min(currentCapacity, resCapacity) / capacityPerPosition * capacityPerPosition : Math.min(currentCapacity, resCapacity);
                randomCapacityList.add(usedCapacity);
                resCapacity -= usedCapacity;
            }
        } while (resCapacity > 0);
        return randomCapacityList;
    }

    private ArrayList<ArrayList<String>> changeFormColumnToRow(ArrayList<ArrayList<String>> dataAsColumn) {
        int rowCount = dataAsColumn.get(0).size();
        int columnCount = dataAsColumn.size();
        ArrayList<ArrayList<String>> dataAsRow = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; ++i) {
            ArrayList<String> rowData = new ArrayList<>(columnCount);
            for (ArrayList<String> columnData : dataAsColumn) {
                rowData.add(columnData.get(i));
            }
            dataAsRow.add(rowData);
        }
        return dataAsRow;
    }
}
