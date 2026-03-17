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
        // Optional dummy values since backend no longer checks GPS
        this.latitude = 0.0;
        this.longitude = 0.0;
    }
}
