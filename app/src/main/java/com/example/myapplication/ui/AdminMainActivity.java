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

import android.widget.ImageView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMainActivity extends AppCompatActivity {

    private TextView tvTotalEmployees, tvTotalDepartments;
    private TextView tvHeaderName, tvHeaderDept, tvHeaderAvatarText;
    private ImageView ivHeaderAvatar;
    private android.view.View btnHeaderNotifications, btnHeaderExtra, containerProfileLink;
    
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
        BottomNavHelper.setupBottomNav(this, R.id.nav_admin);
    }

    private void initViews() {
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvTotalDepartments = findViewById(R.id.tvTotalDepartments);

        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderDept = findViewById(R.id.tvHeaderDept);
        tvHeaderAvatarText = findViewById(R.id.tvHeaderAvatarText);
        ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
        btnHeaderNotifications = findViewById(R.id.btnHeaderNotifications);
        btnHeaderExtra = findViewById(R.id.btnHeaderExtra);
        containerProfileLink = findViewById(R.id.containerProfileLink);

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

        if (cardCompany != null) {
            cardCompany.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminSettingsActivity.class));
            });
        }

        if (btnHeaderNotifications != null) {
            btnHeaderNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }

        if (btnHeaderExtra != null) {
            btnHeaderExtra.setOnClickListener(v -> Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show());
        }

        if (containerProfileLink != null) {
            containerProfileLink.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }
    }

    private void updateTopBar() {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String fullName = prefs.getFullName();
        String deptName = prefs.getDepartmentName();
        Long employeeId = prefs.getEmployeeId();

        if (fullName.isEmpty() && employeeId != -1L) {
            fetchEmployeeDetails(employeeId);
        } else {
            displayUserInfo(fullName, deptName);
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
                    displayUserInfo(emp.getFullName(), emp.getDepartment());
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                displayUserInfo(SharedPrefsManager.getInstance(AdminMainActivity.this).getUsername(), "");
            }
        });
    }

    private void displayUserInfo(String name, String dept) {
        if (tvHeaderName != null) tvHeaderName.setText(name);
        if (tvHeaderDept != null) tvHeaderDept.setText(dept);
        if (tvHeaderAvatarText != null && name != null && !name.isEmpty()) {
            tvHeaderAvatarText.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
    }

    private void fetchStats() {
        ApiService apiService = RetrofitClient.getApiService(this);

        // Fetch Employee Count
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvTotalEmployees.setText(String.valueOf(response.body().size()));
                }
            }

            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {}
        });

        // Fetch Department Count
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override
            public void onResponse(Call<List<Department>> call, Response<List<Department>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvTotalDepartments.setText(String.valueOf(response.body().size()));
                }
            }

            @Override
            public void onFailure(Call<List<Department>> call, Throwable t) {}
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
        BottomNavHelper.setupBottomNav(this, R.id.nav_admin);
        updateTopBar();
        fetchStats();
    }
}
