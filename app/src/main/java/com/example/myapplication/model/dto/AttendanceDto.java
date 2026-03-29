package com.example.myapplication.model.dto;

/**
 * AttendanceDto — DTO request cho chấm công.
 * Response đã tách thành entity Attendance.
 */
public class AttendanceDto {

    public static class CheckInRequest {
        public Long employeeId;
        public Double latitude;
        public Double longitude;
        public CheckInRequest(Long employeeId, Double lat, Double lng) {
            this.employeeId = employeeId;
            this.latitude   = lat;
            this.longitude  = lng;
        }
    }

    public static class CheckOutRequest {
        public Long employeeId;
        public CheckOutRequest(Long employeeId) {
            this.employeeId = employeeId;
        }
    }
}
