package com.peter.search.thread;

import com.peter.search.datametrics.DataMetricFactory;
import com.peter.search.dto.FullImportDataMetrics;
import com.peter.search.pojo.FetchData;
import com.peter.search.service.impl.IndexWriterServiceImpl;

public class DeltaSyncThread extends DataSyncThread {
    @Override
    protected void indexWrite(FetchData fetchData) {
        indexWriter.indexUpdate(serviceTag, indexName, fetchData.getDataList(), true, new IndexWriterServiceImpl.ESRequestHandler() {
            @Override
            public void failed(Integer failedIndex, String errMsg) {
                dataMetricsService.metricsLog(serviceTag, errMsg);
                dataMetricsService.addFailedCount(serviceTag, 1);
                dataMetricsService.addFailedData(serviceTag, fetchData.getDataList().get(failedIndex).getDocData(), errMsg);
            }
            @Override
            public void success(Integer successCount) {
                dataMetricsService.addSuccessCount(serviceTag, successCount);
            }
        });
    }

    @Override
    protected void dataMetrics(FetchData fetchData) {
        FullImportDataMetrics fullImportDataMetrics = DataMetricFactory.getInstance().getFullDataMetrics(serviceTag);
        if(fetchData.getPage().getTotalCount() != fullImportDataMetrics.getTotalCount()){
            // 刷新totalCount
            fullImportDataMetrics.setTotalCount(fetchData.getPage().getTotalCount());
        }
    }
}
