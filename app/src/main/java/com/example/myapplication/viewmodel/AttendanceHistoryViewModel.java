package com.example.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.model.AttendanceRecord;
import com.example.myapplication.model.AttendanceStats;
import com.example.myapplication.repository.AttendanceRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceHistoryViewModel extends ViewModel {
    private final AttendanceRepository repository;
    private final MutableLiveData<AttendanceStats> stats = new MutableLiveData<>();
    private final MutableLiveData<List<AttendanceRecord>> records = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AttendanceHistoryViewModel() {
        this.repository = new AttendanceRepository();
    }

    public LiveData<AttendanceStats> getStats() { return stats; }
    public LiveData<List<AttendanceRecord>> getRecords() { return records; }
    public LiveData<String> getError() { return error; }

    public void loadAttendanceData(Long employeeId, int month, int year) {
        repository.getAttendanceStats(employeeId, month, year, new Callback<AttendanceStats>() {
            @Override
            public void onResponse(Call<AttendanceStats> call, Response<AttendanceStats> response) {
                if (response.isSuccessful() && response.body() != null) {
                    stats.setValue(response.body());
                } else {
                    error.setValue("Không thể tải thống kê");
                }
            }

            @Override
            public void onFailure(Call<AttendanceStats> call, Throwable t) {
                error.setValue(t.getMessage());
            }
        });

        repository.getAttendanceMonthly(employeeId, month, year, new Callback<List<AttendanceRecord>>() {
            @Override
            public void onResponse(Call<List<AttendanceRecord>> call, Response<List<AttendanceRecord>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    records.setValue(response.body());
                } else {
                    error.setValue("Không thể tải danh sách điểm danh");
                }
            }

            @Override
            public void onFailure(Call<List<AttendanceRecord>> call, Throwable t) {
                error.setValue(t.getMessage());
            }
        });
    }
}