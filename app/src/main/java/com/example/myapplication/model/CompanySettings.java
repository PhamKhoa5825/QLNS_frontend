package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class CompanySettings {
    @SerializedName("id")
    public Long id;

    @SerializedName("companyName")
    public String companyName;

    @SerializedName("baseLat")
    public Double baseLat;

    @SerializedName("baseLng")
    public Double baseLng;

    @SerializedName("allowedRadius")
    public Integer allowedRadius;

    @SerializedName("workStartTime")
    public String workStartTime;

    @SerializedName("morningEndTime")
    public String morningEndTime;

    @SerializedName("afternoonStartTime")
    public String afternoonStartTime;

    @SerializedName("workEndTime")
    public String workEndTime;
}
