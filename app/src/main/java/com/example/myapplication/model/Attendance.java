package com.example.myapplication.model;

public class Attendance {
    private Long id;
    private Long employeeId;
    private String checkIn;
    private String checkOut;
    private String status;

    public Long getId() { return id; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public String getStatus() { return status; }
}
