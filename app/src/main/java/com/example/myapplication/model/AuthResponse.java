package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("tokenType")
    private String tokenType;

    @SerializedName("departmentId")
    private Long departmentId;

    @SerializedName("employeeId")
    private Long employeeId;

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getTokenType() { return tokenType; }
    public Long getDepartmentId() { return departmentId; }
    public Long getEmployeeId() { return employeeId; }
}
