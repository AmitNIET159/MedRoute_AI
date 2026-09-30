package com.medroute.model;

public class GeocodingResult {
    private Double latitude;
    private Double longitude;
    private String displayName;
    private String addressJson;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAddressJson() {
        return addressJson;
    }

    public void setAddressJson(String addressJson) {
        this.addressJson = addressJson;
    }
}
