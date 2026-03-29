package com.example.myapplication.model.dto;

/**
 * AdminDto — DTO request cho cài đặt công ty.
 * Response (CompanySettings, SystemLog) đã tách thành entity.
 */
public class AdminDto {

    public static class UpdateSettingsRequest {
        public String companyName;
        public Double baseLat;
        public Double baseLng;
        public Integer allowedRadius;
        public String workStartTime;
        public String workEndTime;
    }
}
