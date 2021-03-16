package com.peter.search.datametrics;

import com.peter.search.dto.DeltaImportDataMetrics;
import com.peter.search.dto.FullImportDataMetrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DataMetricFactory {

    private static volatile DataMetricFactory instance;

    private static Map<String, FullImportDataMetrics> fullDataMetricMap = new HashMap(16);
    private static Map<String, DeltaImportDataMetrics> deltaDataMetricMap = new HashMap(16);
    private static Map<String, Queue<SearchRequestDataMetrics>> searchRequestDataMetricMap = new HashMap(32);

    public static DataMetricFactory getInstance(){
        if(instance == null){
            synchronized (DataMetricFactory.class){
                if(instance == null){
                    instance = new DataMetricFactory();
                }
            }
        }
        return instance;
    }

    public FullImportDataMetrics getFullDataMetrics(String serviceTag){
        if(!fullDataMetricMap.containsKey(serviceTag)){
            fullDataMetricMap.put(serviceTag, new FullImportDataMetrics(serviceTag));
        }
        return fullDataMetricMap.get(serviceTag);
    }

    public DeltaImportDataMetrics getDeltaDataMetrics(String serviceTag){
        if(!deltaDataMetricMap.containsKey(serviceTag)){
            deltaDataMetricMap.put(serviceTag, new DeltaImportDataMetrics(serviceTag));
        }
        return deltaDataMetricMap.get(serviceTag);
    }

    public Queue<SearchRequestDataMetrics> getSearchRequestDataMetrics(String threadName){
        if(!searchRequestDataMetricMap.containsKey(threadName)){
            searchRequestDataMetricMap.put(threadName, new ConcurrentLinkedQueue<>());
        }
        return searchRequestDataMetricMap.get(threadName);
    }

    public Map<String, Queue<SearchRequestDataMetrics>> getSearchRequestDataMetricMap() {
        return searchRequestDataMetricMap;
    }

    public Map<String, FullImportDataMetrics> getFullDataMetricMap() {
        return fullDataMetricMap;
    }

    public Map<String, DeltaImportDataMetrics> getDeltaDataMetricMap() {
        return deltaDataMetricMap;
    }
}
