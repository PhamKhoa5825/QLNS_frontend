package com.example.myapplication.model;

public class AttendanceModels {

    public static class AttendanceResponse {
        public Long id;
        public String date;
        public String checkIn;
        public String checkOut;
        public Double workHours;
        public String status;       // ON_TIME / LATE / ABSENT
        public Integer lateMinutes;
    }

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