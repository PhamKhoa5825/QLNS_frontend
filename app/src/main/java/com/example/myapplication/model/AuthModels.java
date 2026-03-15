package com.example.myapplication.model;

public class AuthModels {

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