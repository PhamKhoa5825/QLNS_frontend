package com.example.myapplication.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.model.Attendance;
import com.example.myapplication.repository.AttendanceRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceViewModel extends ViewModel {
    private final AttendanceRepository repository = new AttendanceRepository();

    public MutableLiveData<List<Attendance>> todayAttendance = new MutableLiveData<>();
    public MutableLiveData<Attendance> attendanceActionResponse = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public void getTodayAttendance() {
        isLoading.setValue(true);
        repository.getTodayAttendance(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(@NonNull Call<List<Attendance>> call, @NonNull Response<List<Attendance>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    todayAttendance.setValue(response.body());
                } else {
                    errorMessage.setValue(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Attendance>> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void checkIn(Map<String, Object> data) {
        isLoading.setValue(true);
        repository.checkIn(data, new Callback<Attendance>() {
            @Override
            public void onResponse(@NonNull Call<Attendance> call, @NonNull Response<Attendance> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    attendanceActionResponse.setValue(response.body());
                } else {
                    errorMessage.setValue(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Attendance> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    public void checkOut(Map<String, Object> data) {
        isLoading.setValue(true);
        repository.checkOut(data, new Callback<Attendance>() {
            @Override
            public void onResponse(@NonNull Call<Attendance> call, @NonNull Response<Attendance> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    attendanceActionResponse.setValue(response.body());
                } else {
                    errorMessage.setValue(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Attendance> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private String parseError(Response<?> response) {
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) {
                String errorJson = errorBody.string();
                // Sử dụng phương thức tĩnh parseString của JsonParser
                JsonObject jsonObject = JsonParser.parseString(errorJson).getAsJsonObject();
                if (jsonObject.has("message")) {
                    return jsonObject.get("message").getAsString();
                }
            }
        } catch (Exception e) {
            // Log lỗi nếu cần thiết
        }
        return "Lỗi hệ thống (" + response.code() + ")";
    }
}
