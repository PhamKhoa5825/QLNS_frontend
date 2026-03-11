package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.Department;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DepartmentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DepartmentAdapter adapter;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;

    private static final String PREF_NAME = "qlns_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_demo);

        // Nút back
        findViewById(R.id.btnBackDept).setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        fabAdd      = findViewById(R.id.fabAddDept);

        recyclerView = findViewById(R.id.recyclerViewDept);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DepartmentAdapter(new java.util.ArrayList<>(), dept -> {
            // Click vào phòng ban → mở danh sách nhân viên theo phòng ban
            // TODO: mở EmployeeActivity với filter deptId
            Toast.makeText(this, "Phòng: " + dept.getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerView.setAdapter(adapter);

        // Chỉ ADMIN và MANAGER mới thấy nút thêm
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String role = prefs.getString("role", "EMPLOYEE");
        if ("ADMIN".equals(role)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> showAddDepartmentDialog());
        } else {
            fabAdd.setVisibility(View.GONE);
        }

        loadDepartments();
    }

    private void loadDepartments() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getApiService().getDepartments().enqueue(new Callback<List<Department>>() {
            @Override
            public void onResponse(Call<List<Department>> call, Response<List<Department>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                } else {
                    Toast.makeText(DepartmentActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Department>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Log.e("DEPT_API", t.getMessage());
                Toast.makeText(DepartmentActivity.this, "Không kết nối được server", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddDepartmentDialog() {
        // TODO TV1 implement: dialog nhập tên, mô tả phòng ban
        Toast.makeText(this, "Tính năng thêm phòng ban", Toast.LENGTH_SHORT).show();
    }
}