package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class AttendanceRecord {
    @SerializedName("date")
    private String date;
    
    @SerializedName("checkIn")
    private String checkIn;
    
    @SerializedName("checkOut")
    private String checkOut;
    
    @SerializedName("workHours")
    private double workHours;
    
    @SerializedName("status")
    private String status; // ON_TIME, LATE, ABSENT
    
    @SerializedName("lateMinutes")
    private int lateMinutes;

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public double getWorkHours() { return workHours; }
    public void setWorkHours(double workHours) { this.workHours = workHours; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(int lateMinutes) { this.lateMinutes = lateMinutes; }
}