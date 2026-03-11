package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.DepartmentDashboardDTO;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.myapplication.R;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout btnNavEmployee, btnNavTimekeeping, btnNavDepartment, btnNavChat, btnNavTask, btnNavNotification, btnNavRequest;
    private TextView tvTotalEmployees, tvTimekeepingOnTime, tvTimekeepingLate, tvUserName, tvAvatar;
    private ImageView btnLogout;
    
    private Long currentDeptId; // Removed hardcoded 1L

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();

        btnNavEmployee = findViewById(R.id.btnNavEmployee);
        btnNavTimekeeping = findViewById(R.id.btnNavTimekeeping);
        btnNavDepartment = findViewById(R.id.btnNavDepartment);
        btnNavChat = findViewById(R.id.btnNavChat);
        btnNavTask = findViewById(R.id.btnNavTask);
        btnNavNotification = findViewById(R.id.btnNavNotification);
        btnNavRequest = findViewById(R.id.btnNavRequest); // Added new button for Requests
        
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvTimekeepingOnTime = findViewById(R.id.tvTimekeepingOnTime);
        tvTimekeepingLate = findViewById(R.id.tvTimekeepingLate);
        tvUserName = findViewById(R.id.tvUserName);
        tvAvatar = findViewById(R.id.tvAvatar);
        btnLogout = findViewById(R.id.btnLogout);
        
        updateUserUI();

        btnNavEmployee.setOnClickListener(v -> startActivity(new Intent(this, EmployeeActivity.class)));
        btnNavDepartment.setOnClickListener(v -> startActivity(new Intent(this, DepartmentActivity.class)));
        btnNavTask.setOnClickListener(v -> startActivity(new Intent(this, TaskActivity.class)));
        btnNavTimekeeping.setOnClickListener(v -> startActivity(new Intent(this, TimekeepingActivity.class)));
        btnNavChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        btnNavNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        
        if (btnNavRequest != null) {
            btnNavRequest.setOnClickListener(v -> startActivity(new Intent(this, RequestActivity.class)));
        }

        btnLogout.setOnClickListener(v -> {
            SharedPrefsManager.getInstance(this).logout();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        
        fetchDashboardStats();
    }
    
    private void fetchDashboardStats() {
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<DepartmentDashboardDTO> call = apiService.getDepartmentDashboard(currentDeptId);
        
        call.enqueue(new Callback<DepartmentDashboardDTO>() {
            @Override
            public void onResponse(Call<DepartmentDashboardDTO> call, Response<DepartmentDashboardDTO> response) {
                if(response.isSuccessful() && response.body() != null) {
                    DepartmentDashboardDTO dto = response.body();
                    tvTotalEmployees.setText(String.valueOf(dto.getTotalEmployees()));
                    tvTimekeepingOnTime.setText(String.valueOf(dto.getPresentToday()));
                    tvTimekeepingLate.setText(String.valueOf(dto.getLateToday()));
                }
            }

            @Override
            public void onFailure(Call<DepartmentDashboardDTO> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Lỗi fetch dashboard stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserUI() {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String username = prefs.getUsername();
        if (username != null && !username.isEmpty()) {
            tvUserName.setText(username);
            tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
        }
    }
}
