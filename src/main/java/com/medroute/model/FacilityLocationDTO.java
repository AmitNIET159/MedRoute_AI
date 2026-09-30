package com.medroute.model;

public class FacilityLocationDTO {
    private Facility facility;
    private Double distanceKm;
    private String currentRisk;
    private Integer criticalItems;
    private WeatherSnapshot weather;

    public Facility getFacility() {
        return facility;
    }

    public void setFacility(Facility facility) {
        this.facility = facility;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getCurrentRisk() {
        return currentRisk;
    }

    public void setCurrentRisk(String currentRisk) {
        this.currentRisk = currentRisk;
    }

    public Integer getCriticalItems() {
        return criticalItems;
    }

    public void setCriticalItems(Integer criticalItems) {
        this.criticalItems = criticalItems;
    }

    public WeatherSnapshot getWeather() {
        return weather;
    }

    public void setWeather(WeatherSnapshot weather) {
        this.weather = weather;
    }
}
