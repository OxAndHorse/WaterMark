package cn.ac.iie.pkcgroup.dws.core.db.utils;

import cn.ac.iie.pkcgroup.dws.core.db.model.BasicTraceInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.DatasetWithPK;
import cn.ac.iie.pkcgroup.dws.core.db.model.PartitionedDataset;
import cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.opt.SecretKeyGenerator;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;

import java.util.ArrayList;

/****
 * 数据集进行划分
 */
public class PartitionUtils {
    public static final int M = 65535;// 字符串Hashcode分布范围, M >> wmLength

    /****
     * @apiNote BKDRHash算法，对字符数组进行散列，结果取绝对值
     * @param s 字符数组
     * @return 字符数组对应的Hashcode，非负
     */
    private static int BKDRHash(char[] s) {
        int seed = 131;// 31 131 1313 13131 131313 etc..
        int hash = 0;
        for (char c : s) {
            hash = hash * seed + c;
        }
        return Math.abs(hash % M);
    }

    /****
     * @apiNote 根据公式H(Ks | | H ( p.r | | Ks))，计算获取Mac值
     * @param primaryKey 主键
     * @param keyCode 密钥
     * @return Mac值
     */
    private static int getMac(String primaryKey, String keyCode) {
        char[] stageOne = (primaryKey + keyCode).toCharArray();
        String tmp = keyCode + BKDRHash(stageOne);
        return BKDRHash(tmp.toCharArray());
    }

    public static String generatePartitionKey(BasicTraceInfo basicTraceInfo) {
        ArrayList<String> materialList = new ArrayList<>();
        materialList.add(basicTraceInfo.getSystemId());
        materialList.add(basicTraceInfo.getDbName());
        materialList.add(basicTraceInfo.getTableName());
        materialList.add(basicTraceInfo.getFieldModel().getSelectedField().getFieldName());
        return SecretKeyGenerator.generateSecretKey(StringUtils.generateIdenticalKey(materialList));
    }
    /****
     * @apiNote 数据集划分
     * @param data 数据集，Map类型，key为数据主键，value为主键对应列数据（类型为ArrayList）
     * @param secretKey 密钥
     * @return 划分后的数据集map，Map类型，key为划分集合下标，value为ArrayList，包含该集合下所有列数据（类型为ArrayList）
     */
    public static PartitionedDataset divide(DatasetWithPK data, String secretKey) {
        PartitionedDataset partitionedDataset = new PartitionedDataset();
        for (String key : data.getDataset().keySet()) {
            int mac = getMac(key, secretKey);
            int index = mac % M;
            ArrayList<String> value = data.getDataset().get(key);
            ArrayList<ArrayList<String>> tmp;
            if (partitionedDataset.containsIndex(index)) {
                tmp = partitionedDataset.getPartitionByIndex(index);
                tmp.add(value);
            } else {
                tmp = new ArrayList<>();
                tmp.add(value);
                partitionedDataset.addToPartition(index, tmp);
            }
        }
        return partitionedDataset;
    }

}
