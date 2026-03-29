package com.example.myapplication.model;

import java.io.Serializable;

public class RequestDetail implements Serializable {
    private Long id;
    private String specificDate; // yyyy-MM-dd
    private LeaveSession leaveSession;
    private Double overtimeHours;
    private String checkIn;  // HH:mm
    private String checkOut; // HH:mm

    public RequestDetail() {}

    public RequestDetail(String specificDate, LeaveSession leaveSession, Double overtimeHours) {
        this.specificDate = specificDate;
        this.leaveSession = leaveSession;
        this.overtimeHours = overtimeHours;
    }

    public RequestDetail(String specificDate, String checkIn, String checkOut) {
        this.specificDate = specificDate;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSpecificDate() { return specificDate; }
    public void setSpecificDate(String specificDate) { this.specificDate = specificDate; }
    public LeaveSession getLeaveSession() { return leaveSession; }
    public void setLeaveSession(LeaveSession leaveSession) { this.leaveSession = leaveSession; }
    public Double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Double overtimeHours) { this.overtimeHours = overtimeHours; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
}
