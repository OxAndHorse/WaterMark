package cn.ac.iie.pkcgroup.dws.core.db.processor.numeric.opt;

import cn.ac.iie.pkcgroup.dws.config.algorithms.db.PatternSearchConf;
import cn.ac.iie.pkcgroup.dws.core.db.model.ConstraintType;
import cn.ac.iie.pkcgroup.dws.utils.SpringContextUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;

/**
 * 模式搜索--最优化
 */
@Slf4j
public class PatternSearch {
    @Data
    private static class SearchState {
        private ArrayList<Double> initState; //初始状态
        private ArrayList<Double> recordState; //中间状态
        private ArrayList<Double> changeRecord; //增向量
        private double ref;
    }

    @Data
    public static class SearchResult {
        private ArrayList<Double> initState;
        double result;
    }

    private final PatternSearchConf patternSearchConf;

    public PatternSearch() {
        patternSearchConf = (PatternSearchConf) SpringContextUtils.getContext().getBean("patternSearchConf");
    }

    public double computeSigmoid(PatternSearchConf patternSearchConf, double x, double ref) {
        double alpha = patternSearchConf == null ? this.patternSearchConf.getAlpha() : patternSearchConf.getAlpha();
        return (1.0 - 1.0/(1 + Math.exp(alpha * (x-ref))));
    }

    private double setNextStepLength(double currentLength) {
        return currentLength * patternSearchConf.getDecayRate();
    }

    private boolean cmp(PatternSearchConf patternSearchConf, double x, double y, boolean isMax) {
        if (isMax) {
            return (x-y) > patternSearchConf.getExp();
        } else {
            return (x-y) < patternSearchConf.getExp();
        }
    }

    private double getOptimizedResult(SearchState searchState, PatternSearchConf patternSearchConf) {
        double sum = 0;
        double ref = searchState.getRef();
        for(double i: searchState.getRecordState()){
            sum += computeSigmoid(patternSearchConf, i, ref);
        }
        return sum / searchState.getRecordState().size();
    }

    /**
     * 逐步搜索
     */
    private void stepByStep(SearchState searchState, PatternSearchConf patternSearchConf, double stepLength, boolean isMax) {
        double upper = patternSearchConf.getUpper();
        double lower = patternSearchConf.getLower();
        double ref = searchState.getRef();
        ArrayList<Double>tmp = new ArrayList<>(searchState.getRecordState());
        int len = tmp.size();
        for (int i = 0; i < len; i++) {
            double valueI = tmp.get(i);
            if (cmp(patternSearchConf, computeSigmoid(patternSearchConf, valueI + stepLength, ref), computeSigmoid(patternSearchConf, valueI, ref), isMax) && searchState.getChangeRecord().get(i) < upper) {
                tmp.set(i, valueI + stepLength);
                searchState.getChangeRecord().set(i, searchState.getChangeRecord().get(i) + stepLength);
            } else if (cmp(patternSearchConf, computeSigmoid(patternSearchConf, valueI - stepLength, ref), computeSigmoid(patternSearchConf, valueI, ref), isMax) && searchState.getChangeRecord().get(i) > lower) {
                tmp.set(i, valueI - stepLength);
                searchState.getChangeRecord().set(i, searchState.getChangeRecord().get(i) - stepLength);
            }
        }
        searchState.setRecordState(tmp);
    }

    /**
     * 每回合搜索TURN_NUM次数
     */
    private double searchByAxis(SearchState searchState, PatternSearchConf patternSearchConf, double stepLength, boolean isMax) {
        for(int i=0; i < patternSearchConf.getTurnNum(); i++) {
            stepByStep(searchState, patternSearchConf, stepLength, isMax);
            stepLength = setNextStepLength(stepLength);
        }
        return stepLength;
    }

    /**
     * 加速搜索
     */
    private void searchByPattern(SearchState searchState, PatternSearchConf patternSearchConf, boolean isMax) {
        ArrayList<Double>tmp = new ArrayList<>(searchState.getRecordState());
        ArrayList<Double>tmpChange = new ArrayList<>(searchState.getChangeRecord());
        double upper = patternSearchConf.getUpper();
        double lower = patternSearchConf.getLower();
        double ref = searchState.getRef();
        int len = tmp.size();
        boolean ok = true;
        double x = 0, y = 0;
        for(int i = 0; i < len; i++) {
            if(searchState.getChangeRecord().get(i) > upper || searchState.getChangeRecord().get(i) < lower){
                ok = false;
                break;
            }
            tmp.set(i, searchState.getRecordState().get(i) + patternSearchConf.getAccurate() * (searchState.getRecordState().get(i)-searchState.getInitState().get(i)));
            x += computeSigmoid(patternSearchConf, tmp.get(i), ref);
            y += computeSigmoid(patternSearchConf, searchState.getRecordState().get(i), ref);
            tmpChange.set(i, tmpChange.get(i) + patternSearchConf.getAccurate() * (searchState.getRecordState().get(i)-searchState.getInitState().get(i)));
        }

        if (cmp(patternSearchConf, x, y, isMax) && ok) {
            searchState.setChangeRecord(tmpChange);
            searchState.setInitState(tmp);
            searchState.setRecordState(tmp);
        } else {
            searchState.setInitState(searchState.getRecordState());
        }
    }

    /**
     * 模式搜索
     * 分为按向量搜索
     * 加速搜索
     */
    private void search(SearchState searchState, PatternSearchConf patternSearchConf, double initStepLength, boolean isMax) {
        double stepLength = initStepLength;
        while((stepLength - patternSearchConf.getPrecision()) > patternSearchConf.getExp()) {
            stepLength = searchByAxis(searchState, patternSearchConf, stepLength, isMax);
            searchByPattern(searchState, patternSearchConf, isMax);
        }
    }

//    public ArrayList<Double> getResult(){
//        return this.initState;
//    }

    /**
     * 获取最大最优化后的隐藏函数均值meanMax
     * @param colValues 一列数据
     * @param ref 参考值
     * @return 隐藏函数最大值
     */
    public SearchResult maximizeByHidingFunction(ArrayList<Double> colValues, double ref, PatternSearchConf patternSearchConf) {
        return patternSearchConf != null ? initParams(colValues, ref, patternSearchConf, true) : initParams(colValues, ref, this.patternSearchConf, true);
    }

    private SearchResult initParams(ArrayList<Double> colValues, double ref, PatternSearchConf patternSearchConf, boolean isMax) {
        SearchState searchState = new SearchState();
        searchState.setInitState(colValues);
        searchState.setRecordState(colValues);
        searchState.setChangeRecord(new ArrayList<>(Collections.nCopies(colValues.size(), 0.0)));
        double lower = patternSearchConf.getLower();
        double upper = patternSearchConf.getUpper();
        searchState.setRef(ref);
        double stepLength = Math.max(Math.abs(lower), Math.abs(upper)) / 2;
        search(searchState, patternSearchConf, stepLength, isMax);
        double result = getOptimizedResult(searchState, patternSearchConf);
        SearchResult searchResult = new SearchResult();
        searchResult.setInitState(searchState.getInitState());
        searchResult.setResult(result);
        return searchResult;
    }

    /**
     * 获取最小最优化后的隐藏函数均值meanMin
     * @param colValues 一列数据
     * @param ref 参考值
     * @return 隐藏函数最小值
     */
    public SearchResult minimizeByHidingFunction(ArrayList<Double> colValues, double ref, PatternSearchConf patternSearchConf) {
        return patternSearchConf != null ? initParams(colValues, ref, patternSearchConf, false) : initParams(colValues, ref, this.patternSearchConf, false) ;
    }

    public String formatOutput(PatternSearchConf patternSearchConf, boolean isDouble) {
        PatternSearchConf conf = patternSearchConf != null ? patternSearchConf : this.patternSearchConf;
        // 精度约束
        String precision = Double.toString(conf.getPrecision());
        int len;
        if (precision.startsWith("0")){
            // 小数点后几位
            len = precision.length()-2;
        } else {
            len = 0;
        }
        // 格式化输出的占位符设置
        String placeholder = "%.0f";
        if (isDouble) {
            placeholder = "%." + len + "f";
        }
        return placeholder;
    }
}
