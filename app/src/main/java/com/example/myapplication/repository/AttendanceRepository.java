package com.example.myapplication.repository;

import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.AttendanceRecord;
import com.example.myapplication.model.AttendanceStats;
import com.example.myapplication.network.RetrofitClient;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;

public class AttendanceRepository {
    public void getTodayAttendance(Callback<List<Attendance>> callback) {
        RetrofitClient.getApiService().getTodayAttendance().enqueue(callback);
    }

    public void checkIn(Map<String, Object> data, Callback<Attendance> callback) {
        RetrofitClient.getApiService().checkIn(data).enqueue(callback);
    }

    public void checkOut(Map<String, Object> data, Callback<Attendance> callback) {
        RetrofitClient.getApiService().checkOut(data).enqueue(callback);
    }

    public void getAttendanceStats(Long id, int month, int year, Callback<AttendanceStats> callback) {
        RetrofitClient.getApiService().getAttendanceStats(id, month, year).enqueue(callback);
    }

    public void getAttendanceMonthly(Long id, int month, int year, Callback<List<AttendanceRecord>> callback) {
        RetrofitClient.getApiService().getAttendanceMonthly(id, month, year).enqueue(callback);
    }
}