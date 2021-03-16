package com.peter.search.datametrics;

import org.springframework.stereotype.Service;

@Service(value = "fullDataMetricsService")
public class FullDataMetricsServiceImpl implements DataMetricsService{

    @Override
    public void metricsLog(String serviceTag, String logInfo){
        DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).addLog(logInfo);
    }

    @Override
    public void addSuccessCount(String serviceTag, Integer successCount){
        DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).addSuccessCount(successCount);
    }

    @Override
    public void addFailedCount(String serviceTag, Integer failedCount){
        DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).addFailedCount(failedCount);
    }

    @Override
    public void minusFailedCount(String serviceTag, Integer failedCount) {
        throw new IllegalStateException("未实现");
    }

    @Override
    public void addFailedData(String serviceTag, String failedData, String failedReason) {
        DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).addFailedData(failedData, failedReason);
    }

    @Override
    public void addAcceptCount(String serviceTag, Integer acceptCount) {
        DataMetricFactory.getInstance().getFullDataMetrics(serviceTag).addAcceptCount(acceptCount);
    }
}
