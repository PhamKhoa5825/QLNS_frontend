package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Contact {

    @SerializedName("userId")
    private Long userId;

    @SerializedName("employeeId")
    private Long employeeId;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("position")
    private String position;

    @SerializedName("departmentName")
    private String departmentName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("skills")
    private String skills;

    @SerializedName("status")
    private String status;

    public Long getUserId() {
        return userId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPosition() {
        return position;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getSkills() {
        return skills;
    }

    public String getStatus() {
        return status;
    }

    public String getDepartment() {
        return departmentName;
    }
}
