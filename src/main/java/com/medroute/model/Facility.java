package com.medroute.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Facility {
    private Long id;
    private String name;
    private FacilityType facilityType;
    private String registrationNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String email;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean active;
    private BigDecimal reliabilityScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Facility() {
    }

    public Facility(Long id, String name, FacilityType facilityType, String registrationNumber, String address, String city, String state, String pincode, String phone, String email, BigDecimal latitude, BigDecimal longitude, boolean active, BigDecimal reliabilityScore, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.facilityType = facilityType;
        this.registrationNumber = registrationNumber;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.phone = phone;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = active;
        this.reliabilityScore = reliabilityScore;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FacilityType getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(FacilityType facilityType) {
        this.facilityType = facilityType;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public BigDecimal getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(BigDecimal reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Facility{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", facilityType=" + facilityType +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", pincode='" + pincode + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", active=" + active +
                ", reliabilityScore=" + reliabilityScore +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
