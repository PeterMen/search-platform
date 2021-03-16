package com.peter.search.pojo;

/**
 * 地理查询参数
 *
 * @author 王海涛
 * */
public class GeoParam {

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getiGeoSort() {
        return iGeoSort;
    }

    public void setiGeoSort(String iGeoSort) {
        this.iGeoSort = iGeoSort;
    }

    private Double lat;
    private Double lng;
    private Integer distanceMeters;
    private String iGeoSort;
}
