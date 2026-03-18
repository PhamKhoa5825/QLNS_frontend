package com.example.myapplication.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.EmployeeSummary;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.viewmodel.AttendanceViewModel;
import com.example.myapplication.viewmodel.EmployeeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeEmployeeActivity extends AppCompatActivity {

    private TextView tvUserName, tvAvatarInitials, tvCurrentTime, tvCurrentDate;
    private MaterialButton btnCheckIn;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private Runnable timeRunnable;
    
    private AttendanceViewModel attendanceViewModel;
    private EmployeeViewModel employeeViewModel;
    private boolean isCheckedIn = false;
    private long currentUserId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_employee);

        RetrofitClient.init(this);
        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);
        employeeViewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);
        currentUserId = SharedPrefsManager.getInstance(this).getUserId();

        initViews();
        startClock();
        observeViewModel();
        employeeViewModel.loadEmployeeSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        attendanceViewModel.getTodayAttendance();
    }

    private void observeViewModel() {
        attendanceViewModel.todayAttendance.observe(this, attendances -> {
            updateAttendanceUI(attendances);
        });

        attendanceViewModel.errorMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        employeeViewModel.employeeSummary.observe(this, summary -> {
            if (summary != null) {
                updateUserNameUI(summary);
            }
        });

        employeeViewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserNameUI(EmployeeSummary summary) {
        if (summary.getFullName() != null) {
            tvUserName.setText(summary.getFullName());
        }
        if (summary.getAvatarText() != null) {
            tvAvatarInitials.setText(summary.getAvatarText());
        }
    }

    private void updateAttendanceUI(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            showCheckInState();
            return;
        }

        boolean hasCurrentUserRecord = false;
        boolean hasOpenAttendance = false;

        for (Attendance attendance : attendances) {
            if (attendance == null) continue;

            Long employeeId = attendance.getEmployeeId();
            if (currentUserId != -1L && employeeId != null && !employeeId.equals(currentUserId)) {
                continue;
            }

            hasCurrentUserRecord = true;
            String checkOut = attendance.getCheckOut();
            if (checkOut == null || checkOut.trim().isEmpty()) {
                hasOpenAttendance = true;
                break;
            }
        }

        if (!hasCurrentUserRecord) {
            showCheckInState();
        } else if (hasOpenAttendance) {
            isCheckedIn = true;
            btnCheckIn.setText("Check-out");
            btnCheckIn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
            btnCheckIn.setEnabled(true);
        } else {
            isCheckedIn = true;
            btnCheckIn.setText("Đã hoàn thành");
            btnCheckIn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
            btnCheckIn.setEnabled(false);
        }
    }

    private void showCheckInState() {
        isCheckedIn = false;
        btnCheckIn.setText("Check-in");
        btnCheckIn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A73E8")));
        btnCheckIn.setEnabled(true);
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        btnCheckIn = findViewById(R.id.btnCheckIn);

        btnCheckIn.setOnClickListener(v -> {
            Intent intent = new Intent(HomeEmployeeActivity.this, GPSCheckInActivity.class);
            intent.putExtra("isCheckInAction", !isCheckedIn);
            startActivity(intent);
        });

        findViewById(R.id.btnNotification).setOnClickListener(v -> startActivity(new Intent(this, NotificationCenterActivity.class)));
        setupNavigation();
    }

    private void setupNavigation() {
        ViewGroup menuGrid = findViewById(R.id.menuGrid);
        if (menuGrid != null) {
            // Xin nghỉ phép (Card 1)
            menuGrid.getChildAt(0).setOnClickListener(v -> startActivity(new Intent(this, RequestListActivity.class)));

            // Lịch sử điểm danh (Card 2)
            menuGrid.getChildAt(1).setOnClickListener(v -> {
                Intent intent = new Intent(this, AttendanceHistoryActivity.class);
                startActivity(intent);
            });
            
            // Thông báo (Card 3)
            menuGrid.getChildAt(2).setOnClickListener(v -> startActivity(new Intent(this, NotificationCenterActivity.class)));
        }

        findViewById(R.id.imgAvatar).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_work) {
                    startActivity(new Intent(this, TaskManagementActivity.class));
                } else if (itemId == R.id.nav_message) {
                    startActivity(new Intent(this, ChatActivity.class));
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
                return true;
            });
        }
    }

    private void startClock() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd 'tháng' M, yyyy", new Locale("vi", "VN"));
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                tvCurrentTime.setText(timeFormat.format(now));
                tvCurrentDate.setText(dateFormat.format(now));
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeHandler.removeCallbacks(timeRunnable);
    }
}
