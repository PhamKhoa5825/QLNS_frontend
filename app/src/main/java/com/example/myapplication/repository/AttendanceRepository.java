package com.example.myapplication.repository;

import android.content.Context;

import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.AttendanceRecord;
import com.example.myapplication.model.AttendanceStats;
import com.example.myapplication.model.AttendanceStatsResponse;
import com.example.myapplication.model.CompanySettings;
import com.example.myapplication.model.TimekeepingRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.util.List;
import retrofit2.Callback;

public class AttendanceRepository {
    private ApiService apiService;

    public AttendanceRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
    }

    public void getTodayAttendance(Long empId, Callback<Attendance> callback) {
        apiService.getTodayAttendance(empId).enqueue(callback);
    }

    public void checkIn(TimekeepingRequest data, Callback<Attendance> callback) {
        apiService.checkIn(data).enqueue(callback);
    }

    public void checkOut(TimekeepingRequest data, Callback<Attendance> callback) {
        apiService.checkOut(data).enqueue(callback);
    }

    public void getAttendanceStats(Long id, int month, int year, Callback<AttendanceStats> callback) {
        apiService.getAttendanceStats(id, month, year).enqueue(callback);
    }

    public void getAttendanceMonthly(Long id, int month, int year, Callback<List<AttendanceRecord>> callback) {
        apiService.getAttendanceMonthly(id, month, year).enqueue(callback);
    }

    public void getCompanySettings(Callback<CompanySettings> callback) {
        apiService.getCompanySettings().enqueue(callback);
    }
}