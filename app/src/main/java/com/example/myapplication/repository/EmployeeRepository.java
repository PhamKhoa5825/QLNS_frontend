package com.example.myapplication.repository;

import android.content.Context;
import android.util.Log;

import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.EmployeeSummary;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeRepository {
    private final ApiService apiService;
    private final SharedPrefsManager prefsManager;

    public EmployeeRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
        this.prefsManager = SharedPrefsManager.getInstance(context);
    }

    public boolean hasToken() {
        String token = prefsManager.getToken();
        return token != null && !token.isEmpty();
    }

    public void clearToken() {
        prefsManager.logout();
    }

    public Long getSavedUserId() {
        return prefsManager.getUserId();
    }

    public void getEmployeeSummary(Long id, RepositoryCallback<EmployeeSummary> callback) {
        if (id == null || id <= 0) {
            callback.onError("ID nhân viên không hợp lệ");
            return;
        }
        
        Log.d("EmployeeRepo", "Fetching summary for id: " + id);
        apiService.getEmployeeSummary(id).enqueue(new Callback<EmployeeSummary>() {
            @Override
            public void onResponse(Call<EmployeeSummary> call, Response<EmployeeSummary> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Lỗi tải thông tin (Code: " + response.code() + ")";
                    Log.e("EmployeeRepo", errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<EmployeeSummary> call, Throwable t) {
                Log.e("EmployeeRepo", "Network error fetching summary", t);
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void getMyProfile(RepositoryCallback<Employee> callback) {
        apiService.getMyProfile().enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    if (response.code() == 401) {
                        callback.onError("UNAUTHORIZED");
                    } else {
                        callback.onError("Lỗi tải thông tin cá nhân");
                    }
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void getEmployeeDetail(Long id, RepositoryCallback<Employee> callback) {
        apiService.getEmployeeDetail(id).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Lỗi tải chi tiết nhân viên");
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void updateEmployeeProfile(Long id, Map<String, Object> data, RepositoryCallback<Employee> callback) {
        apiService.updateEmployeeProfile(id, data).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Lỗi cập nhật thông tin");
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void getEmployees(RepositoryCallback<List<Employee>> callback) {
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e("EmployeeRepo", "Call list failed: Code " + response.code() + " - Body: " + errorBody);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    callback.onError("Lỗi: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                Log.e("EmployeeRepo", "Network error in getEmployees", t);
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void searchEmployees(String keyword, RepositoryCallback<List<Employee>> callback) {
        apiService.searchEmployees(keyword).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Lỗi tìm kiếm");
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void getDepartments(RepositoryCallback<List<Department>> callback) {
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override
            public void onResponse(Call<List<Department>> call, Response<List<Department>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Lỗi tải danh sách phòng ban");
                }
            }

            @Override
            public void onFailure(Call<List<Department>> call, Throwable t) {
                callback.onError("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
