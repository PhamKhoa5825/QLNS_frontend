package com.example.myapplication.model;
import com.google.gson.annotations.SerializedName;

public class EmployeeAttendanceStats {
    @SerializedName("employeeId")
    private Long employeeId;
    @SerializedName("employeeName")
    private String employeeName;
    @SerializedName("onTime")
    private long onTime;
    @SerializedName("late")
    private long late;
    @SerializedName("absent")
    private long absent;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public long getOnTime() { return onTime; }
    public void setOnTime(long onTime) { this.onTime = onTime; }
    public long getLate() { return late; }
    public void setLate(long late) { this.late = late; }
    public long getAbsent() { return absent; }
    public void setAbsent(long absent) { this.absent = absent; }
}
