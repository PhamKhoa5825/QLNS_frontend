package com.example.myapplication.model;
import com.google.gson.annotations.SerializedName;

public class TimekeepingRequest {
    @SerializedName("employeeId")
    private Long employeeId;
    @SerializedName("latitude")
    private Double latitude;
    @SerializedName("longitude")
    private Double longitude;

    public TimekeepingRequest(Long employeeId) {
        this.employeeId = employeeId;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    public TimekeepingRequest(Long employeeId, Double latitude, Double longitude) {
        this.employeeId = employeeId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getEmployeeId() { return employeeId; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
