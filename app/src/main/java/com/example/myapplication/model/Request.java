package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Request {

    @SerializedName("id")
    private Long id;

    @SerializedName(value = "title", alternate = {"requestTitle", "name"})
    private String title;

    @SerializedName(value = "description", alternate = {"content", "reason"})
    private String description;

    @SerializedName(value = "status", alternate = {"requestStatus"})
    private String status;

    @SerializedName(value = "createdAt", alternate = {"createdDate", "createdTime"})
    private String createdAt;

    @SerializedName(value = "employeeId", alternate = {"empId"})
    private Long employeeId;

    @SerializedName(value = "employeeCode", alternate = {"empCode", "employeeNo"})
    private String employeeCode;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title == null ? "(Không có tiêu đề)" : title;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public String getStatus() {
        return status == null ? "PENDING" : status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }
}

