package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Attendance {
    @SerializedName("id")
    private Long id;

    @SerializedName("date")
    private String date;

    @SerializedName("checkInTime")
    private String checkInTime;

    @SerializedName("checkOutTime")
    private String checkOutTime;

    @SerializedName("status")
    private String status; // PRESENT, LATE, ABSENT, LEAVE

    @SerializedName("employeeId")
    private Long employeeId;

    @SerializedName("employeeName")
    private String employeeName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }

    public String getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
}
