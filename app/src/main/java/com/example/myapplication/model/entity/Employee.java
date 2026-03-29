package com.example.myapplication.model.entity;

import com.google.gson.annotations.SerializedName;

public class Employee {

    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;            // THÊM: User.id (cho account management)

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

    @SerializedName("accountStatus")
    private String accountStatus;   // THÊM: ACTIVE / INACTIVE (trạng thái tài khoản)

    @SerializedName("departmentId")
    private Long departmentId;

    @SerializedName("departmentName")
    private String departmentName;

    @SerializedName("role")
    private String role;            // EMPLOYEE / MANAGER / ADMIN

    // ===== Hàm tiện ích =====

    public String getFullName() { return fullName; }

    // Adapter cũ dùng getRole() → trả position (chức vụ)
    public String getRole() { return position; }

    // Adapter cũ dùng getDepartment() → trả tên phòng ban
    public String getDepartment() {
        return departmentName != null ? departmentName : "";
    }

    // Adapter cũ dùng getStatus() → trả chuỗi tiếng Việt
    public String getStatus() {
        return "ACTIVE".equals(status) ? "Đang làm việc" : "Đã nghỉ việc";
    }

    public String getAvatarText() {
        if (fullName != null && !fullName.isEmpty()) {
            String trimmed = fullName.trim();
            if (!trimmed.isEmpty()) {
                String[] parts = trimmed.split(" ");
                String last = parts[parts.length - 1];
                return !last.isEmpty() ? String.valueOf(last.charAt(0)).toUpperCase() : "?";
            }
        }
        return "?";
    }

    public boolean isWorking() { return "ACTIVE".equals(status); }

    // Trạng thái tài khoản đăng nhập
    public boolean isAccountActive() {
        return accountStatus == null || "ACTIVE".equalsIgnoreCase(accountStatus);
    }

    public String getAccountStatusDisplay() {
        return isAccountActive() ? "Hoạt động" : "Bị khoá";
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender() { return gender; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPosition() { return position; }
    public String getJoinDate() { return joinDate; }
    public String getStatusRaw() { return status; }
    public String getAccountStatus() { return accountStatus; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getRoleRaw() { return role; }
}