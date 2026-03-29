package com.example.myapplication.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.CompanySettings;
import com.example.myapplication.repository.AttendanceRepository;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.model.TimekeepingRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceViewModel extends AndroidViewModel {
    private final AttendanceRepository repository;

    public AttendanceViewModel(@NonNull Application application) {
        super(application);
        this.repository = new AttendanceRepository(application);
    }

    public MutableLiveData<Attendance> todayAttendance = new MutableLiveData<>();
    public MutableLiveData<Attendance> attendanceActionResponse = new MutableLiveData<>();
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    public MutableLiveData<CompanySettings> companySettings = new MutableLiveData<>();

    public void fetchTodayAttendance(Long empId) {
        isLoading.setValue(true);
        repository.getTodayAttendance(empId, new Callback<Attendance>() {
            @Override
            public void onResponse(@NonNull Call<Attendance> call, @NonNull Response<Attendance> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    todayAttendance.setValue(response.body());
                } else {
                    errorMessage.setValue(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Attendance> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void checkIn(com.example.myapplication.model.TimekeepingRequest data) {
        isLoading.setValue(true);
        repository.checkIn(data, new Callback<Attendance>() {
            @Override
            public void onResponse(@NonNull Call<Attendance> call, @NonNull Response<Attendance> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    attendanceActionResponse.setValue(response.body());
                    todayAttendance.setValue(response.body());
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

    public void checkOut(com.example.myapplication.model.TimekeepingRequest data) {
        isLoading.setValue(true);
        repository.checkOut(data, new Callback<Attendance>() {
            @Override
            public void onResponse(@NonNull Call<Attendance> call, @NonNull Response<Attendance> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    attendanceActionResponse.setValue(response.body());
                    todayAttendance.setValue(response.body());
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
                // Sử dụng cách tương thích với các phiên bản Gson cũ hơn
                JsonObject jsonObject = new JsonParser().parse(errorJson).getAsJsonObject();
                if (jsonObject.has("message")) {
                    return jsonObject.get("message").getAsString();
                }
            }
        } catch (Exception e) {
            // Log lỗi nếu cần thiết
        }
        return "Lỗi hệ thống (" + response.code() + ")";
    }

    // Được gọi từ GPSCheckInActivity để tải cấu hình công ty (bán kính GPS, v.v.)
    public void fetchCompanySettings(android.content.Context context) {
        com.example.myapplication.network.ApiService api = com.example.myapplication.network.RetrofitClient.getApiService(context);
        api.getCompanySettings().enqueue(new retrofit2.Callback<CompanySettings>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<CompanySettings> call, @NonNull retrofit2.Response<CompanySettings> response) {
                if (response.isSuccessful() && response.body() != null) {
                    companySettings.setValue(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<CompanySettings> call, @NonNull Throwable t) {
                // Không cần xử lý lỗi nghiêm trọng ở đây
            }
        });
    }
}
