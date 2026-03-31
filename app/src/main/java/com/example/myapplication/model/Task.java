package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Task {
    @SerializedName("id")
    private Long id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("deadline")
    private String deadline;

    @SerializedName("priority")
    private String priority; // LOW, MEDIUM, HIGH

    @SerializedName("status")
    private String status; // PENDING, ACCEPTED, UNDER_REVIEW, DONE, REJECTED, OVERDUE

    @SerializedName("assignedToId")
    private Long assignedToId;

    @SerializedName("assignedToName")
    private String assignedToName;

    @SerializedName("assignedById")
    private Long assignedById;

    @SerializedName("assignedByName")
    private String assignedByName;

    @SerializedName("departmentId")
    private Long departmentId;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("assignedToAvatarUrl")
    private String assignedToAvatarUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public Long getAssignedById() { return assignedById; }
    public void setAssignedById(Long assignedById) { this.assignedById = assignedById; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getAssignedToAvatarUrl() { return assignedToAvatarUrl; }
    public void setAssignedToAvatarUrl(String assignedToAvatarUrl) { this.assignedToAvatarUrl = assignedToAvatarUrl; }
}
