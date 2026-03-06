package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.Department;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// =============================================
// DEPARTMENT ACTIVITY
// =============================================
public class DepartmentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DepartmentAdapter adapter;
    private ProgressBar progressBar;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_demo);

        btnBack = findViewById(R.id.btnBackDept);
        btnBack.setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);  // thêm ProgressBar vào layout nếu chưa có

        recyclerView = findViewById(R.id.recyclerViewDept);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo adapter rỗng trước
        adapter = new DepartmentAdapter(new java.util.ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Gọi API thay vì hardcode
        loadDepartments();
    }

    private void loadDepartments() {
        // Hiện loading
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        ApiService api = RetrofitClient.getApiService();
        api.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override
            public void onResponse(Call<List<Department>> call,
                                   Response<List<Department>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    adapter.setData(response.body());
                } else {
                    Toast.makeText(DepartmentActivity.this,
                            "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Department>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                Log.e("API_ERROR", "Lỗi: " + t.getMessage());  // ← xem trong Logcat
                Toast.makeText(DepartmentActivity.this,
                        t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}