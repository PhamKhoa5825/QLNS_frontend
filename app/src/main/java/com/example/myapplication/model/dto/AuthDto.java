package com.example.myapplication.model.dto;

/**
 * AuthDto — DTO cho authentication.
 * AuthResponse giữ ở đây vì nó là response 1 lần (login),
 * không phải entity lưu trữ.
 */
public class AuthDto {

    public static class LoginRequest {
        public String username;
        public String password;
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class AuthResponse {
        public String token;
        public Long userId;
        public Long employeeId;
        public String username;
        public String role;
        public String fullName;
        public String avatarUrl;
    }
}
