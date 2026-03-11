package com.example.myapplication.model;
import com.google.gson.annotations.SerializedName;

public class AttendanceStatsResponse {
    @SerializedName("onTime")
    private long onTime;
    @SerializedName("late")
    private long late;
    @SerializedName("absent")
    private long absent;

    public long getOnTime() { return onTime; }
    public void setOnTime(long onTime) { this.onTime = onTime; }
    public long getLate() { return late; }
    public void setLate(long late) { this.late = late; }
    public long getAbsent() { return absent; }
    public void setAbsent(long absent) { this.absent = absent; }
}
