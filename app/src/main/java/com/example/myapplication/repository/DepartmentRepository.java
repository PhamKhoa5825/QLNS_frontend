package com.example.myapplication.repository;

import android.content.Context;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DepartmentRepository {
    private final ApiService apiService;

    public DepartmentRepository(Context context) {
        this.apiService = RetrofitClient.getApiService(context);
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

    public void createDepartment(Department dept, RepositoryCallback<Department> callback) {
        apiService.createDepartment(dept).enqueue(new Callback<Department>() {
            @Override
            public void onResponse(Call<Department> call, Response<Department> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Lỗi tạo phòng ban");
                }
            }
            @Override
            public void onFailure(Call<Department> call, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void updateDepartment(Long id, Department dept, RepositoryCallback<Department> callback) {
        apiService.updateDepartment(id, dept).enqueue(new Callback<Department>() {
            @Override
            public void onResponse(Call<Department> call, Response<Department> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Lỗi cập nhật phòng ban");
                }
            }
            @Override
            public void onFailure(Call<Department> call, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void deleteDepartment(Long id, RepositoryCallback<Void> callback) {
        apiService.deleteDepartment(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Lỗi xóa phòng ban");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void getEmployeesByDepartment(Long deptId, RepositoryCallback<List<Employee>> callback) {
        apiService.getEmployeesByDepartment(deptId).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Lỗi tải danh sách nhân viên");
                }
            }
            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
