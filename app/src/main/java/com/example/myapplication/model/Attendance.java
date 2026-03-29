package com.example.myapplication.model;

public class Attendance {
    private Long id;
    private Long employeeId;
    private String checkIn;
    private String checkOut;
    private String date;
    private String status;
    private Integer lateMinutes;
    private Double workHours;

    public Long getEmployeeId() { return employeeId; }
    public Long getId() { return id; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public Integer getLateMinutes() { return lateMinutes; }
    public Double getWorkHours() { return workHours; }

    public String getCheckInTime() {
        if (checkIn == null || checkIn.length() < 16) return null;
        return checkIn.substring(11, 16); // Extract HH:mm from "yyyy-MM-ddTHH:mm:ss"
    }

    public String getCheckOutTime() {
        if (checkOut == null || checkOut.length() < 16) return null;
        return checkOut.substring(11, 16); // Extract HH:mm
    }

    // Setters được dùng khi tạo mock object trong RequestActivity
    public void setDate(String date) { this.date = date; }
    public void setStatus(String status) { this.status = status; }
}
