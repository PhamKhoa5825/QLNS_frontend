package com.example.myapplication.model.dto;

/**
 * AccountDto — DTO request cho quản lý tài khoản (Admin).
 */
public class AccountDto {

    public static class UpdateRoleRequest {
        public String role;     // EMPLOYEE / MANAGER / ADMIN
        public UpdateRoleRequest(String role) {
            this.role = role;
        }
    }

    public static class ResetPasswordRequest {
        public String newPassword;
        public ResetPasswordRequest(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
