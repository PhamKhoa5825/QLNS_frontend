package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.CompanySettings;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.TopBarHelper;

import android.widget.ImageView;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMainActivity extends AppCompatActivity {

    private TextView tvTotalEmployees, tvTotalDepartments;
    private TextView tvPendingRequests, tvOpenTasks, tvTodayAttendance;
    
    // Company Card Views
    private android.view.View cardCompany;
    private TextView tvCompanyName, tvWorkHours, tvRadius, tvLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        initViews();
        setupClickListeners();
        updateTopBar();
        fetchStats();
        
        // Initialize Bottom Navigation
        BottomNavHelper.setupBottomNav(this, -1);
    }
 
    private void initViews() {
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvTotalDepartments = findViewById(R.id.tvTotalDepartments);
        tvPendingRequests = findViewById(R.id.tvPendingRequests);
        tvOpenTasks = findViewById(R.id.tvOpenTasks);
        tvTodayAttendance = findViewById(R.id.tvTodayAttendance);

        tvTodayAttendance = findViewById(R.id.tvTodayAttendance);

        // Company Card
        cardCompany = findViewById(R.id.cardCompany);
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvWorkHours = findViewById(R.id.tvWorkHours);
        tvRadius = findViewById(R.id.tvRadius);
        tvLocation = findViewById(R.id.tvLocation);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnAdminAddEmployee).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditEmployeeActivity.class));
        });
 
        findViewById(R.id.btnAdminAccounts).setOnClickListener(v -> {
            startActivity(new Intent(this, AccountManagementActivity.class));
        });
 
        findViewById(R.id.btnAdminLogs).setOnClickListener(v -> {
            startActivity(new Intent(this, SystemLogActivity.class));
        });
 
        findViewById(R.id.btnAdminSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminSettingsActivity.class));
        });
 
        findViewById(R.id.btnAdminBackup).setOnClickListener(v -> {
            startActivity(new Intent(this, BackupActivity.class));
        });
    }

    private void updateTopBar() {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String fullName = prefs.getFullName();
        Long employeeId = prefs.getEmployeeId();

        if (fullName.isEmpty() && employeeId != -1L) {
            fetchEmployeeDetails(employeeId);
        } else {
            TopBarHelper.setupTopBar(this);
        }
    }

    private void fetchEmployeeDetails(Long employeeId) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Employee emp = response.body();
                    SharedPrefsManager prefs = SharedPrefsManager.getInstance(AdminMainActivity.this);
                    prefs.setFullName(emp.getFullName());
                    prefs.setDepartmentName(emp.getDepartment());
                    TopBarHelper.setupTopBar(AdminMainActivity.this);
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                TopBarHelper.setupTopBar(AdminMainActivity.this);
            }
        });
    }


    private void fetchStats() {
        ApiService apiService = RetrofitClient.getApiService(this);

        // Fetch Comprehensive Admin Dashboard Stats
        apiService.getAdminDashboardStats().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> stats = response.body();
                    
                    // Basic Counts
                    if (stats.containsKey("totalEmployees")) {
                        tvTotalEmployees.setText(String.valueOf(stats.get("totalEmployees")));
                    }
                    if (stats.containsKey("totalDepartments")) {
                        tvTotalDepartments.setText(String.valueOf(stats.get("totalDepartments")));
                    }

                    // Dashboard Specifics
                    if (tvPendingRequests != null) tvPendingRequests.setText(String.valueOf(stats.get("pendingRequests")));
                    if (tvOpenTasks != null) tvOpenTasks.setText(String.valueOf(stats.get("openTasks")));
                    if (tvTodayAttendance != null) tvTodayAttendance.setText(String.valueOf(stats.get("todayAttendance")));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Fallback or error handled silently for now
            }
        });

        // Fetch Company Settings for Card
        apiService.getCompanySettings().enqueue(new Callback<CompanySettings>() {
            @Override
            public void onResponse(Call<CompanySettings> call, Response<CompanySettings> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CompanySettings s = response.body();
                    if (tvCompanyName != null) tvCompanyName.setText(s.companyName != null ? s.companyName : "--");
                    if (tvWorkHours != null) {
                        String time = (s.workStartTime != null ? s.workStartTime : "--") + " → " + 
                                     (s.workEndTime != null ? s.workEndTime : "--");
                        tvWorkHours.setText(time);
                    }
                    if (tvRadius != null) tvRadius.setText(s.allowedRadius != null ? (s.allowedRadius + " m") : "-- m");
                    if (tvLocation != null) {
                        if (s.baseLat != null && s.baseLng != null) {
                            tvLocation.setText(String.format(java.util.Locale.US, "%.5f, %.5f", s.baseLat, s.baseLng));
                        } else {
                            tvLocation.setText("Chưa thiết lập");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<CompanySettings> call, Throwable t) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure the correct tab is highlighted
        BottomNavHelper.setupBottomNav(this, -1);
        TopBarHelper.setupTopBar(this);
        fetchStats();
    }
}
