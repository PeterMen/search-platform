package com.peter.search.datametrics;

import org.springframework.stereotype.Service;

@Service(value = "deltaDataMetricsService")
public class DeltaDataMetricsServiceImpl implements DataMetricsService{

    @Override
    public void metricsLog(String serviceTag, String logInfo){
        DataMetricFactory.getInstance().getDeltaDataMetrics(serviceTag).addLog(logInfo);
    }

    @Override
    public void addSuccessCount(String serviceTag, Integer successCount){
        DataMetricFactory.getInstance().getDeltaDataMetrics(serviceTag).addSuccessCount(successCount);
    }

    @Override
    public void addFailedCount(String serviceTag, Integer failedCount){
        DataMetricFactory.getInstance().getDeltaDataMetrics(serviceTag).addFailedCount(failedCount);
    }

    @Override
    public void addAcceptCount(String serviceTag, Integer acceptCount) {
        DataMetricFactory.getInstance().getDeltaDataMetrics(serviceTag).addAcceptCount(acceptCount);
    }

    @Override
    public void minusFailedCount(String serviceTag, Integer failedCount){
        DataMetricFactory.getInstance().getDeltaDataMetrics(serviceTag).minusFailedCount(failedCount);
    }

    @Override
    public void addFailedData(String serviceTag, String failedData, String failedReason) {
        throw new IllegalStateException("未实现");
    }
}
