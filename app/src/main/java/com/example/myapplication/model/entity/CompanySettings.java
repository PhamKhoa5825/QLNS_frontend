package com.example.myapplication.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * CompanySettings entity — map từ API GET /api/settings
 * Tương ứng backend: com.example.qlns.Entity.CompanySettings
 */
public class CompanySettings {

    @SerializedName("id")
    public Long id;

    @SerializedName("companyName")
    public String companyName;

    @SerializedName("baseLat")
    public Double baseLat;

    @SerializedName("baseLng")
    public Double baseLng;

    @SerializedName("allowedRadius")
    public Integer allowedRadius;

    @SerializedName("workStartTime")
    public String workStartTime;

    @SerializedName("workEndTime")
    public String workEndTime;

    // ── Helpers ──

    public String getWorkHoursDisplay() {
        String start = workStartTime != null ? workStartTime : "--:--";
        String end = workEndTime != null ? workEndTime : "--:--";
        return start + " → " + end;
    }

    public String getRadiusDisplay() {
        return allowedRadius != null ? allowedRadius + " m" : "-- m";
    }

    public String getLocationDisplay() {
        if (baseLat != null && baseLng != null) {
            return String.format(java.util.Locale.US, "%.5f, %.5f", baseLat, baseLng);
        }
        return "Chưa thiết lập";
    }
}
