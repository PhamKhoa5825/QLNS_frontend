package com.example.myapplication.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.DepartmentDashboardDTO;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.service.ChatForegroundService;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.viewmodel.AttendanceViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout btnNavEmployee, btnNavTimekeeping, btnNavDepartment, btnNavChat, btnNavTask, btnNavNotification, btnNavRequest, btnNavPayroll, btnNavSettings;
    private View btnCheckInGPS;
    private TextView tvCheckInStatus, tvTimekeepingOnTime, tvTimekeepingLate;
    private TextView tvHeaderName, tvHeaderDept, tvHeaderAvatarText;
    private View btnHeaderNotifications, btnHeaderExtra, containerProfileLink;
    private ImageView ivHeaderAvatar;
    
    private Long currentDeptId;
    private AttendanceViewModel attendanceViewModel;
    private boolean isCheckInAction = true;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    
    private android.content.BroadcastReceiver systemNotificationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        checkNotificationPermission();
        startBackgroundService();

        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();

        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);

        initViews();
        setupObservers();
        updateTopBar();
        fetchDashboardStats();
        BottomNavHelper.setupBottomNav(this, R.id.nav_home);
        setupNotificationReceiver();
    }

    private void setupNotificationReceiver() {
        systemNotificationReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String title = intent.getStringExtra("title");
                if (title != null) {
                    Toast.makeText(DashboardActivity.this, "Hệ thống: " + title, Toast.LENGTH_LONG).show();
                    fetchDashboardStats();
                }
            }
        };
        android.content.IntentFilter filter = new android.content.IntentFilter("com.example.myapplication.SYSTEM_NOTIFICATION");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemNotificationReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(systemNotificationReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAttendanceStatus();
    }

    private void refreshAttendanceStatus() {
        Long empId = SharedPrefsManager.getInstance(this).getEmployeeId();
        if (empId != -1L) {
            attendanceViewModel.fetchTodayAttendance(empId);
        }
    }

    private void setupObservers() {
        attendanceViewModel.todayAttendance.observe(this, attendance -> {
            if (attendance == null) {
                isCheckInAction = true;
                if (tvCheckInStatus != null) tvCheckInStatus.setText("Bấm để Check-in");
                if (btnCheckInGPS != null) btnCheckInGPS.setEnabled(true);
            } else if (attendance.getCheckIn() != null && attendance.getCheckOut() == null) {
                isCheckInAction = false;
                if (tvCheckInStatus != null) tvCheckInStatus.setText("Bấm để Check-out");
                if (btnCheckInGPS != null) btnCheckInGPS.setEnabled(true);
            } else if (attendance.getCheckOut() != null) {
                if (tvCheckInStatus != null) tvCheckInStatus.setText("Đã hoàn thành chấm công");
                if (btnCheckInGPS != null) btnCheckInGPS.setEnabled(false); // Hoặc giữ để xem lại
            }
        });
    }

    private void initViews() {
        btnNavEmployee = findViewById(R.id.btnNavEmployee);
        btnNavDepartment = findViewById(R.id.btnNavDepartment);
        btnNavTimekeeping = findViewById(R.id.btnNavTimekeeping);
        btnNavChat = findViewById(R.id.btnNavChat);
        btnNavTask = findViewById(R.id.btnNavTask);
        btnNavRequest = findViewById(R.id.btnNavRequest);
        btnNavNotification = findViewById(R.id.btnNavNotification);
        btnNavPayroll = findViewById(R.id.btnNavPayroll);
        btnNavSettings = findViewById(R.id.btnNavSettings);

        btnCheckInGPS = findViewById(R.id.btnCheckInGPS);
        tvCheckInStatus = findViewById(R.id.tvCheckInStatus);
        tvTimekeepingOnTime = findViewById(R.id.tvTimekeepingOnTime);
        tvTimekeepingLate = findViewById(R.id.tvTimekeepingLate);

        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderDept = findViewById(R.id.tvHeaderDept);
        tvHeaderAvatarText = findViewById(R.id.tvHeaderAvatarText);
        ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
        btnHeaderNotifications = findViewById(R.id.btnHeaderNotifications);
        btnHeaderExtra = findViewById(R.id.btnHeaderExtra);
        containerProfileLink = findViewById(R.id.containerProfileLink);

        btnNavEmployee.setOnClickListener(v -> startActivity(new Intent(this, EmployeeActivity.class)));
        btnNavDepartment.setOnClickListener(v -> startActivity(new Intent(this, DepartmentActivity.class)));
        btnNavTask.setOnClickListener(v -> startActivity(new Intent(this, TaskActivity.class)));
        btnNavTimekeeping.setOnClickListener(v -> startActivity(new Intent(this, TimekeepingActivity.class)));
        btnNavChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));

        if (btnCheckInGPS != null) {
            btnCheckInGPS.setOnClickListener(v -> {
                Intent intent = new Intent(this, GPSCheckInActivity.class);
                intent.putExtra("isCheckInAction", isCheckInAction);
                startActivity(intent);
            });
        }
        
        if (btnNavRequest != null) {
            btnNavRequest.setOnClickListener(v -> startActivity(new Intent(this, RequestActivity.class)));
        }

        if (btnNavNotification != null) {
            btnNavNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }

        if (btnNavPayroll != null) {
            btnNavPayroll.setOnClickListener(v -> startActivity(new Intent(this, AdminPayrollActivity.class)));
        }

        if (btnNavSettings != null) {
            btnNavSettings.setOnClickListener(v -> startActivity(new Intent(this, AdminSettingsActivity.class)));
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

        // Hide items based on role
        String role = SharedPrefsManager.getInstance(this).getRole();
        if ("EMPLOYEE".equals(role)) {
            if (btnNavDepartment != null) btnNavDepartment.setVisibility(View.GONE);
        }
        if ("ADMIN".equals(role)) {
            if (btnNavPayroll != null) btnNavPayroll.setVisibility(View.VISIBLE);
            if (btnNavSettings != null) btnNavSettings.setVisibility(View.VISIBLE);
        }
    }

    private void fetchDashboardStats() {
        if (currentDeptId == null || currentDeptId == -1L) {
            return;
        }
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<DepartmentDashboardDTO> call = apiService.getDepartmentDashboard(currentDeptId);
        
        call.enqueue(new Callback<DepartmentDashboardDTO>() {
            @Override
            public void onResponse(Call<DepartmentDashboardDTO> call, Response<DepartmentDashboardDTO> response) {
                if(response.isSuccessful() && response.body() != null) {
                    DepartmentDashboardDTO dto = response.body();
                    if (tvTimekeepingOnTime != null) tvTimekeepingOnTime.setText(String.valueOf(dto.getPresentToday()));
                    if (tvTimekeepingLate != null) tvTimekeepingLate.setText(String.valueOf(dto.getLateToday()));
                }
            }

            @Override
            public void onFailure(Call<DepartmentDashboardDTO> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Lỗi fetch dashboard stats", Toast.LENGTH_SHORT).show();
            }
        });
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
                    SharedPrefsManager prefs = SharedPrefsManager.getInstance(DashboardActivity.this);
                    prefs.setFullName(emp.getFullName());
                    prefs.setDepartmentName(emp.getDepartment());
                    displayUserInfo(emp.getFullName(), emp.getDepartment());
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                displayUserInfo(SharedPrefsManager.getInstance(DashboardActivity.this).getUsername(), "");
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

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, ChatForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        if (systemNotificationReceiver != null) {
            unregisterReceiver(systemNotificationReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã bật thông báo hệ thống", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Thông báo bị tắt, bạn có thể không nhận được tin nhắn mới", Toast.LENGTH_LONG).show();
            }
        }
    }
}
