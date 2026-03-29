package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class AttendanceStats {
    @SerializedName("totalWorkHours")
    private double totalWorkHours;
    
    @SerializedName("onTimeCount")
    private int onTimeCount;
    
    @SerializedName("lateCount")
    private int lateCount;
    
    @SerializedName("averageHoursPerDay")
    private double averageHoursPerDay;

    // Getters and Setters
    public double getTotalWorkHours() { return totalWorkHours; }
    public void setTotalWorkHours(double totalWorkHours) { this.totalWorkHours = totalWorkHours; }
    public int getOnTimeCount() { return onTimeCount; }
    public void setOnTimeCount(int onTimeCount) { this.onTimeCount = onTimeCount; }
    public int getLateCount() { return lateCount; }
    public void setLateCount(int lateCount) { this.lateCount = lateCount; }
    public double getAverageHoursPerDay() { return averageHoursPerDay; }
    public void setAverageHoursPerDay(double averageHoursPerDay) { this.averageHoursPerDay = averageHoursPerDay; }
}