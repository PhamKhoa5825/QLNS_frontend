package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

// Model nhận JSON từ Spring Boot API
// Thay thế Employee.java cũ (hardcode)
public class Employee {

    @SerializedName("id")
    private Long id;

    @SerializedName("employeeCode")
    private String employeeCode;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("position")
    private String position;       // Chức vụ: "Lập trình viên"

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("salary")
    private Double salary;

    @SerializedName("contractType")
    private String contractType;

    @SerializedName("gender")
    private String gender;

    @SerializedName("startDate")
    private String startDate;

    @SerializedName("endDate")
    private String endDate;        // null = còn làm việc

    @SerializedName("department")
    private Department department;

    // ===== Các hàm tiện ích cho UI =====

    // Thay getName() cũ → dùng getFullName()
    public String getFullName() { return fullName; }

    // Thay getRole() cũ → dùng getPosition()
    public String getRole() { return position; }

    // Thay getDepartment() cũ → lấy tên phòng ban
    public String getDepartment() {
        if (department != null) return department.getName();
        return "";
    }

    // Giữ nguyên getStatus() cho Adapter khỏi sửa
    public String getStatus() {
        return isWorking() ? "Đang làm việc" : "Đã nghỉ việc";
    }

    // Giữ nguyên getAvatarText() cho Adapter khỏi sửa
    public String getAvatarText() {
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.trim().split(" ");
            return String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return "?";
    }

    // Giữ nguyên isWorking() cho Adapter khỏi sửa
    public boolean isWorking() { return endDate == null; }

    // Getters
    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getPhone() { return phone; }
    public String getPosition() { return position; }
    public String getAvatarUrl() { return avatarUrl; }
    public Double getSalary() { return salary; }
    public String getContractType() { return contractType; }
    public String getGender() { return gender; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Department getDepartmentObject() { return department; }
}