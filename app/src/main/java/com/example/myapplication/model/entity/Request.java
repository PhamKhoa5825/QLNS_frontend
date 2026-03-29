package com.example.myapplication.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * Request entity — map từ API GET /api/requests
 * Tương ứng backend: com.example.qlns.DTO.Response.RequestDTO
 */
public class Request {

    @SerializedName("id")
    public Long id;

    @SerializedName("employeeId")
    public Long employeeId;

    @SerializedName("employeeName")
    public String employeeName;

    @SerializedName("departmentName")
    public String departmentName;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("fileUrl")
    public String fileUrl;

    @SerializedName("fileName")
    public String fileName;

    @SerializedName("status")
    public String status;               // PENDING / APPROVED / REJECTED

    @SerializedName("reviewedById")
    public Long reviewedById;

    @SerializedName("reviewedByName")
    public String reviewedByName;

    @SerializedName("rejectionReason")
    public String rejectionReason;

    @SerializedName("targetRole")
    public String targetRole;           // MANAGER / ADMIN

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("updatedAt")
    public String updatedAt;

    // ── Helpers ──

    public boolean isPending()  { return "PENDING".equals(status); }
    public boolean isApproved() { return "APPROVED".equals(status); }
    public boolean isRejected() { return "REJECTED".equals(status); }

    public String getStatusDisplay() {
        if (status == null) return "—";
        switch (status) {
            case "PENDING":  return "Chờ duyệt";
            case "APPROVED": return "Đã duyệt";
            case "REJECTED": return "Từ chối";
            default:         return status;
        }
    }

    public String getAvatarText() {
        if (employeeName == null || employeeName.isEmpty()) return "?";
        String[] parts = employeeName.trim().split(" ");
        String last = parts[parts.length - 1];
        return !last.isEmpty() ? String.valueOf(last.charAt(0)).toUpperCase() : "?";
    }
}
