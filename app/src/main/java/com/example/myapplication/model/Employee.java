package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

// Model nhận JSON từ Spring Boot API
public class Employee {

    @SerializedName("id")
    private Long id;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("dateOfBirth")
    private String dateOfBirth;

    @SerializedName("gender")
    private String gender;          // MALE / FEMALE / OTHER

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("position")
    private String position;        // Chức vụ

    @SerializedName("joinDate")
    private String joinDate;

    @SerializedName("status")
    private String status;          // ACTIVE / RESIGNED

    @SerializedName("departmentId")
    private Long departmentId;

    @SerializedName("departmentName")
    private String departmentName;

    @SerializedName("role")
    private String role;            // EMPLOYEE / MANAGER / ADMIN

    // ===== Hàm tiện ích giữ nguyên để Adapter không phải sửa =====

    public String getFullName() { return fullName; }

    // Adapter cũ dùng getRole() → vẫn trả về position (chức vụ)
    public String getRole() { return position; }

    // Adapter cũ dùng getDepartment() → trả về tên phòng ban
    public String getDepartment() {
        return departmentName != null ? departmentName : "";
    }

    // Adapter cũ dùng getStatus() → trả về chuỗi tiếng Việt
    public String getStatus() {
        return "ACTIVE".equals(status) ? "Đang làm việc" : "Đã nghỉ việc";
    }

    // Adapter cũ dùng getAvatarText() → lấy chữ cái đầu
    public String getAvatarText() {
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.trim().split(" ");
            return String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return "?";
    }

    // Adapter cũ dùng isWorking() → dựa vào status
    public boolean isWorking() { return "ACTIVE".equals(status); }

    // Getters mới
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPosition() { return position; }
    public String getJoinDate() { return joinDate; }
    public String getStatusRaw() { return status; }     // ACTIVE / RESIGNED
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getRoleRaw() { return role; }         // EMPLOYEE / MANAGER / ADMIN
}