package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.*;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private ProgressBar progressBar;
    private ImageView btnBack;

    // Lưu toàn bộ list để filter
    private List<Employee> allEmployees;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_demo);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);

        recyclerView = findViewById(R.id.recyclerViewEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAdapter(new java.util.ArrayList<>(),
                employee -> showEmployeeDialog(employee));
        recyclerView.setAdapter(adapter);

        loadEmployees();
    }

    private void loadEmployees() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        ApiService api = RetrofitClient.getApiService();
        api.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call,
                                   Response<List<Employee>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    allEmployees = response.body();
                    adapter.setData(allEmployees);
                } else {
                    Toast.makeText(EmployeeActivity.this,
                            "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(EmployeeActivity.this,
                        "Không kết nối được server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Tìm kiếm nhân viên theo tên
    public void searchEmployee(String keyword) {
        if (keyword.isEmpty()) {
            adapter.setData(allEmployees);
            return;
        }

        ApiService api = RetrofitClient.getApiService();
        api.searchEmployees(keyword).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call,
                                   Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                Toast.makeText(EmployeeActivity.this,
                        "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Lọc theo phòng ban
    public void filterByDepartment(Long departmentId) {
        ApiService api = RetrofitClient.getApiService();
        api.getEmployeesByDepartmentId(departmentId).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call,
                                   Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) { }
        });
    }

    private void showEmployeeDialog(Employee employee) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_employee_detail, null);

        android.widget.TextView tvAvatar = view.findViewById(R.id.tvDialogAvatar);
        android.widget.TextView tvName   = view.findViewById(R.id.tvDialogName);
        android.widget.TextView tvRole   = view.findViewById(R.id.tvDialogRole);

        tvAvatar.setText(employee.getAvatarText());
        tvName.setText(employee.getFullName());     // đổi getName() → getFullName()
        tvRole.setText(employee.getRole());

        dialog.setContentView(view);
        dialog.show();
    }
}