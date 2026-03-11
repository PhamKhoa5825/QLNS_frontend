package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class CreateTaskRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("priority")
    private String priority; // "HIGH", "MEDIUM", "LOW", "URGENT"

    @SerializedName("deadline")
    private String deadline; // "2026-04-01"

    @SerializedName("attachmentUrl")
    private String attachmentUrl;

    @SerializedName("assignedToId")
    private Long assignedToId;

    @SerializedName("assignedById")
    private Long assignedById;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }

    public Long getAssignedById() { return assignedById; }
    public void setAssignedById(Long assignedById) { this.assignedById = assignedById; }
}
