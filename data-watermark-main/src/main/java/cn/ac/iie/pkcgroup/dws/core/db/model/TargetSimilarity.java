package cn.ac.iie.pkcgroup.dws.core.db.model;

import lombok.Data;

@Data
public class TargetSimilarity implements Comparable<TargetSimilarity> {
    // 目标信息
    String targetInfo;
    // 相似度
    double similarity;

    public TargetSimilarity(String tagetInfo, double similarity) {
        this.targetInfo = tagetInfo;
        this.similarity = similarity;
    }

    @Override
    public int compareTo(TargetSimilarity o) {
        Double oDouble = o.similarity;
        return oDouble.compareTo(this.similarity);
    }


    @Override
    public String toString() {
        return "TargetSimilarity{" +
                "targetInfo='" + targetInfo + '\'' +
                ", similarity=" + similarity +
                '}';
    }
}
