package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class AdminModels {

    /** Cài đặt công ty - nhận từ GET /api/settings */
    public static class CompanySettings {
        public Long id;
        public String companyName;
        public Double baseLat;
        public Double baseLng;
        public Integer allowedRadius;
        public String workStartTime;
        public String workEndTime;
    }

    /** Request cập nhật cài đặt - gửi PUT /api/settings */
    public static class UpdateSettingsRequest {
        public String companyName;
        public Double baseLat;
        public Double baseLng;
        public Integer allowedRadius;
        public String workStartTime;
        public String workEndTime;
    }

    /** Log hệ thống - nhận từ GET /api/admin/logs */
    public static class SystemLogResponse {
        public Long id;
        public String username;
        public String action;
        public String description;
        public String createdAt;
        public String department;   // Phòng ban (trả từ backend)
        public String position;     // Chức vụ (trả từ backend)
    }

    /** Dùng cho autocomplete trong filter dialog */
    public static class EmployeeResponse {
        public Long id;
        public String fullName;
        public String departmentName;
        public String position;
    }
}