package com.example.myapplication.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * Attendance entity — map từ API GET /api/attendance
 * Tương ứng backend: com.example.qlns.Entity.Attendance
 */
public class Attendance {

    @SerializedName("id")
    public Long id;

    @SerializedName("date")
    public String date;

    @SerializedName("checkIn")
    public String checkIn;

    @SerializedName("checkOut")
    public String checkOut;

    @SerializedName("workHours")
    public Double workHours;

    @SerializedName("status")
    public String status;           // ON_TIME / LATE / ABSENT

    @SerializedName("lateMinutes")
    public Integer lateMinutes;

    // ── Helpers ──

    public boolean isLate()   { return "LATE".equals(status); }
    public boolean isOnTime() { return "ON_TIME".equals(status); }
    public boolean isAbsent() { return "ABSENT".equals(status); }

    public String getStatusDisplay() {
        if (status == null) return "—";
        switch (status) {
            case "ON_TIME": return "Đúng giờ";
            case "LATE":    return "Đi muộn";
            case "ABSENT":  return "Vắng mặt";
            default:        return status;
        }
    }

    /** Lấy giờ check-in (HH:mm) từ datetime string */
    public String getCheckInTime() {
        return extractTime(checkIn);
    }

    /** Lấy giờ check-out (HH:mm) từ datetime string */
    public String getCheckOutTime() {
        if (checkOut == null) return "Chưa";
        return extractTime(checkOut);
    }

    private String extractTime(String datetime) {
        if (datetime == null) return "—";
        if (datetime.contains("T") && datetime.length() >= 16)
            return datetime.substring(11, 16);
        if (datetime.length() >= 16)
            return datetime.substring(11, 16);
        return datetime;
    }
}
