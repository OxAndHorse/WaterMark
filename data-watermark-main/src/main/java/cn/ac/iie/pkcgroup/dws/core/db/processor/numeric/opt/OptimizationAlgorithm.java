package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.opt;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;

/**
 * 最优化算法抽
 */
@Data
@Slf4j
public class OptimizationAlgorithm {
    private PatternSearch patternSearch;

    public OptimizationAlgorithm() {
        patternSearch = new PatternSearch();
    }

    public double getOHidingValue(ArrayList<Double> colValues, double oref){
        double sum = 0.0;
        for (double i:colValues) {
            // Set conf to null for temporary usage
            sum += patternSearch.computeSigmoid(null, i, oref);
        }
        return sum / (double)colValues.size();
    }

    /**
     * 计算最优化的阈值T,为了防止精度丢失导致的巨大误差
     * 此处，阈值计算约为meanMax与meanMin的均值
     * @param minList 最小值集合
     * @param maxList 最大值集合
     * @return double 最优化的阈值T
     */
    public double calcOptimizedThreshold(ArrayList<Double> minList, ArrayList<Double> maxList)
    {
        DescriptiveStatistics minStats = new DescriptiveStatistics();
        DescriptiveStatistics maxStats = new DescriptiveStatistics();
        minList.forEach(minStats::addValue);
        maxList.forEach(maxStats::addValue);

        double minMean = minStats.getMean();
        double maxMean = maxStats.getMean();
//        double minVar = minStats.getVariance();
//        double maxVar = maxStats.getVariance();
//        log.info("min均值：{}, 方差：{}", minMean, minVar);
//        log.info("max均值：{}, 方差：{}", maxMean, maxVar);

        return (minMean+maxMean)/2;
    }

    /**
     * @apiNote 返回一元二次方程较小的根
     * @return double
     */
    private double getSmallerRootForQuad(double A, double B, double C){
        return ((-B - Math.sqrt(B*B - 4*A*C))/(2*A));
    }

    private double getRootForLinear(double A, double B){
        return (-B)/A;
    }
}
