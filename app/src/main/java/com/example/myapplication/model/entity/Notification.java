package com.example.myapplication.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * Notification entity — map từ API GET /api/notifications
 * Tương ứng backend: com.example.qlns.DTO.Response.NotificationDTO
 */
public class Notification {

    @SerializedName("id")
    public Long id;

    @SerializedName("title")
    public String title;

    @SerializedName("content")
    public String content;

    @SerializedName("targetType")
    public String targetType;       // COMPANY / DEPARTMENT / EMPLOYEE

    @SerializedName("departmentId")
    public Long departmentId;

    @SerializedName("departmentName")
    public String departmentName;

    @SerializedName("createdByName")
    public String createdByName;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("isRead")
    public boolean isRead;

    // ── Helpers ──

    public String getTargetTypeDisplay() {
        if (targetType == null) return "Toàn công ty";
        switch (targetType) {
            case "DEPARTMENT": return departmentName != null ? departmentName : "Phòng ban";
            case "EMPLOYEE":   return "Cá nhân";
            default:           return "Toàn công ty";
        }
    }

    public String getTimeDisplay() {
        if (createdAt == null || createdAt.length() < 16) return "";
        try { return createdAt.substring(11, 16); }
        catch (Exception e) { return createdAt; }
    }
}
