package com.peter.search.datametrics;

public interface DataMetricsService {

    void metricsLog(String serviceTag, String logInfo);

    void addSuccessCount(String serviceTag, Integer successCount);

    void addFailedCount(String serviceTag, Integer failedCount);

    void addAcceptCount(String serviceTag, Integer acceptCount);

    void minusFailedCount(String serviceTag, Integer failedCount);

    void addFailedData(String serviceTag, String failedData, String failedReason);
}
