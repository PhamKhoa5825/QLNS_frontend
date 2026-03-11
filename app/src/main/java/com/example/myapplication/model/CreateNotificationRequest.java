package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class CreateNotificationRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("targetType")
    private String targetType; // "COMPANY" or "DEPARTMENT"

    @SerializedName("departmentId")
    private Long departmentId;

    @SerializedName("createdById")
    private Long createdById;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }
}
