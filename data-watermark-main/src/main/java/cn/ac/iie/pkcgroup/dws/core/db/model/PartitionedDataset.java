package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 划分好的的数据集，用于添加水印
 */
@Data
public class PartitionedDataset {

    Map<Integer, ArrayList<ArrayList<String>>> partitionedDataset;
    int totalCount;

    public PartitionedDataset() {
        partitionedDataset = new HashMap<>();
    }

    public void addToPartition(Integer index, ArrayList<ArrayList<String>> dataRow) {
        partitionedDataset.put(index, dataRow);
    }

    public boolean containsIndex(Integer index){
        return partitionedDataset.containsKey(index);
    }

    public ArrayList<ArrayList<String>> getPartitionByIndex(Integer index){
        return partitionedDataset.get(index);
    }

    public Map<Integer, ArrayList<ArrayList<String>>> getPartitionedDataset() {
        return partitionedDataset;
    }

}


