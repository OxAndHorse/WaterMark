package cn.ac.iie.pkcgroup.dws.config.algorithms.db;

import lombok.Data;

@Data
public class PatternSearchConf {
    private double precision; //搜索终止时的步长精度
    private double decayRate; //衰减速率
    private double accurate; //加速速度
    private int turnNum; //搜索回合数
    private double alpha;//sigmoid函数参数
    private double exp;
    private double upper; //上界
    private double lower; //下界
}
