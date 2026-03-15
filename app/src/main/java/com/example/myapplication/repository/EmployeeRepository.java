package com.example.myapplication.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeRepository {

    private final ApiService apiService;
    private final SharedPreferences prefs;

    public EmployeeRepository(Context context) {
        RetrofitClient.init(context);
        this.apiService = RetrofitClient.getClient().create(ApiService.class);
        this.prefs = context.getSharedPreferences("qlns_pref", Context.MODE_PRIVATE);
    }

    public boolean hasToken() {
        return prefs.getString("token", null) != null;
    }

    public void clearToken() {
        prefs.edit().remove("token").apply();
    }

    public void getEmployees(RepositoryCallback<List<Employee>> callback) {
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                if (r.isSuccessful() && r.body() != null) callback.onSuccess(r.body());
                else callback.onError("Lỗi tải danh sách nhân viên: " + r.code());
            }
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void searchEmployees(String keyword, RepositoryCallback<List<Employee>> callback) {
        apiService.searchEmployees(keyword).enqueue(new Callback<List<Employee>>() {
            @Override public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                if (r.isSuccessful() && r.body() != null) callback.onSuccess(r.body());
                else callback.onError("Lỗi tìm kiếm");
            }
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void getDepartments(RepositoryCallback<List<Department>> callback) {
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override public void onResponse(Call<List<Department>> c, Response<List<Department>> r) {
                if (r.isSuccessful() && r.body() != null) callback.onSuccess(r.body());
                else callback.onError("Lỗi tải phòng ban");
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void createDepartment(Department dept, RepositoryCallback<Department> callback) {
        apiService.createDepartment(dept).enqueue(new Callback<Department>() {
            @Override public void onResponse(Call<Department> c, Response<Department> r) {
                if (r.isSuccessful() && r.body() != null) callback.onSuccess(r.body());
                else callback.onError("Không thể tạo phòng ban");
            }
            @Override public void onFailure(Call<Department> c, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void updateDepartment(Long id, Department dept, RepositoryCallback<Department> callback) {
        apiService.updateDepartment(id, dept).enqueue(new Callback<Department>() {
            @Override public void onResponse(Call<Department> c, Response<Department> r) {
                if (r.isSuccessful() && r.body() != null) callback.onSuccess(r.body());
                else callback.onError("Không thể cập nhật phòng ban");
            }
            @Override public void onFailure(Call<Department> c, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public void deleteDepartment(Long id, RepositoryCallback<Void> callback) {
        apiService.deleteDepartment(id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) callback.onSuccess(null);
                else callback.onError("Không thể xóa phòng ban");
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {
                callback.onError("Lỗi kết nối");
            }
        });
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}