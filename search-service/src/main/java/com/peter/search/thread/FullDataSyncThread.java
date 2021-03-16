package com.peter.search.thread;

import com.peter.search.datametrics.DataMetricFactory;
import com.peter.search.dto.FullImportDataMetrics;
import com.peter.search.pojo.FetchData;
import com.peter.search.service.impl.IndexWriterServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FullDataSyncThread extends DataSyncThread{


    @Override
    public void indexWrite(FetchData fetchData) {
        indexWriter.indexInsert(serviceTag, indexName, fetchData.getDataList(), true, new IndexWriterServiceImpl.ESRequestHandler() {
            @Override
            public void failed(Integer failedIndex, String errMsg) {
                StringBuilder msg = new StringBuilder();
                msg.append(errMsg)
                        .append(",currentPage:")
                        .append(fetchData.getPage() != null ? fetchData.getPage().getCurrentPage() : null)
                        .append("  docId:")
                        .append(fetchData.getDataList().get(failedIndex).getDocId());
                dataMetricsService.metricsLog(serviceTag, msg.toString());
                dataMetricsService.addFailedCount(serviceTag, 1);
            }
            @Override
            public void success(Integer successCount) {
                dataMetricsService.addSuccessCount(serviceTag, successCount);
            }
        });
    }

    @Override
    public void dataMetrics(FetchData fetchData) {
        FullImportDataMetrics fullImportDataMetrics = DataMetricFactory.getInstance().getFullDataMetrics(serviceTag);
        if(fetchData.getPage().getTotalCount() != fullImportDataMetrics.getTotalCount()){
            // 刷新totalCount
            fullImportDataMetrics.setTotalCount(fetchData.getPage().getTotalCount());
        }
    }
}
