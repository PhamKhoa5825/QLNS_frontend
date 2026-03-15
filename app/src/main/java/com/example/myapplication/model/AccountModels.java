package com.example.myapplication.model;

public class AccountModels {

    public static class ResetPasswordRequest {
        public String newPassword;
        public ResetPasswordRequest(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class UpdateRoleRequest {
        public String role;  // EMPLOYEE / MANAGER / ADMIN
        public UpdateRoleRequest(String role) {
            this.role = role;
        }
    }

    // Dùng cho màn hình AccountManagement - hiển thị user + employee info cùng nhau
    public static class AccountInfo {
        public Long userId;
        public Long employeeId;
        public String username;
        public String email;
        public String fullName;
        public String role;
        public String status;       // ACTIVE / INACTIVE
        public String departmentName;
        public String position;
    }
}