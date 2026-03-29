package com.example.myapplication.model.entity;

import com.google.gson.annotations.SerializedName;

/**
 * Task entity — map từ API GET /api/tasks
 * Tương ứng backend: com.example.qlns.DTO.Response.TaskDTO
 */
public class Task {

    @SerializedName("id")
    public Long id;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("priority")
    public String priority;         // LOW / MEDIUM / HIGH / URGENT

    @SerializedName("status")
    public String status;           // PENDING / ACCEPTED / DONE / OVERDUE

    @SerializedName("deadline")
    public String deadline;

    @SerializedName("assignedToName")
    public String assignedToName;

    @SerializedName("assignedToId")
    public Long assignedToId;

    @SerializedName("assignedById")
    public Long assignedById;

    @SerializedName("assignedByName")
    public String assignedByName;

    // ── Helpers ──

    public boolean isPending()  { return "PENDING".equals(status); }
    public boolean isAccepted() { return "ACCEPTED".equals(status); }
    public boolean isDone()     { return "DONE".equals(status); }
    public boolean isOverdue()  { return "OVERDUE".equals(status); }

    public String getPriorityDisplay() {
        if (priority == null) return "—";
        switch (priority) {
            case "URGENT": return "Khẩn cấp";
            case "HIGH":   return "Cao";
            case "MEDIUM": return "Trung bình";
            case "LOW":    return "Thấp";
            default:       return priority;
        }
    }

    public String getStatusDisplay() {
        if (status == null) return "—";
        switch (status) {
            case "PENDING":  return "Chờ nhận";
            case "ACCEPTED": return "Đang làm";
            case "DONE":     return "Hoàn thành";
            case "OVERDUE":  return "Quá hạn";
            default:         return status;
        }
    }
}
