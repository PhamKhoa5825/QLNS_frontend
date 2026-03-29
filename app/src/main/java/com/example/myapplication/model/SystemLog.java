package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

/**
 * SystemLog entity — map từ API GET /api/admin/logs
 * Tương ứng backend: com.example.qlns.Entity.SystemLog
 */
public class SystemLog {

    @SerializedName("id")
    public Long id;

    @SerializedName("username")
    public String username;

    @SerializedName("action")
    public String action;           // LOGIN / CREATE / UPDATE / DELETE / LOGOUT / BACKUP / REVIEW

    @SerializedName("description")
    public String description;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("department")
    public String department;

    @SerializedName("position")
    public String position;

    // ── Helpers ──

    public String getActionIcon() {
        if (action == null || action.isEmpty()) return "?";
        return String.valueOf(action.charAt(0));
    }

    public String getTimeDisplay() {
        if (createdAt == null) return "";
        try {
            String date = createdAt.substring(8, 10) + "/" + createdAt.substring(5, 7);
            String time = createdAt.substring(11, 16);
            return date + " " + time;
        } catch (Exception e) { return createdAt; }
    }

    public String getUserDisplay() {
        return username != null ? username : "System";
    }
}
