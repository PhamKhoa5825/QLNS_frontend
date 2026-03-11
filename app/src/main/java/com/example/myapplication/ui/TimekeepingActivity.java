package com.example.myapplication.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.AttendanceAdapter;
import com.example.myapplication.adapter.EmployeeStatsAdapter;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.AttendanceStatsResponse;
import com.example.myapplication.model.EmployeeAttendanceStats;
import com.example.myapplication.model.TimekeepingRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimekeepingActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvCurrentTime, tvCurrentDate;
    private TextView tabCheckIn, tabHistory, tabDepartment;
    private LinearLayout layoutCheckIn, layoutHistory, layoutDepartment;
    
    private TextView tvStatsOnTime, tvStatsLate, tvStatsAbsent;
    private TextView tvCheckInTimeInfo, tvCheckOutTimeInfo;
    private Button btnCheckIn, btnCheckOut;

    private RecyclerView rvHistory, rvDepartment;
    private AttendanceAdapter historyAdapter;
    private EmployeeStatsAdapter departmentAdapter;

    private Long currentEmployeeId;
    private Long currentDeptId;
    private String currentRole;

    private ApiService apiService;
    private int currentMonth, currentYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timekeeping);

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        currentEmployeeId = prefs.getEmployeeId();
        currentDeptId = prefs.getDepartmentId();
        currentRole = prefs.getRole();
        apiService = RetrofitClient.getApiService(this);

        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1; // 1-based
        currentYear = cal.get(Calendar.YEAR);

        initViews();
        setupTabs();
        setupActionButtons();
        updateDateTime();
        
        // Initial data load
        fetchEmployeeStats();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackTimekeeping);
        btnBack.setOnClickListener(v -> finish());

        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);

        tabCheckIn = findViewById(R.id.tabCheckIn);
        tabHistory = findViewById(R.id.tabHistory);
        tabDepartment = findViewById(R.id.tabDepartment);

        layoutCheckIn = findViewById(R.id.layoutCheckIn);
        layoutHistory = findViewById(R.id.layoutHistory);
        layoutDepartment = findViewById(R.id.layoutDepartment);

        tvStatsOnTime = findViewById(R.id.tvStatsOnTime);
        tvStatsLate = findViewById(R.id.tvStatsLate);
        tvStatsAbsent = findViewById(R.id.tvStatsAbsent);
        
        tvCheckInTimeInfo = findViewById(R.id.tvCheckInTimeInfo);
        tvCheckOutTimeInfo = findViewById(R.id.tvCheckOutTimeInfo);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnCheckOut = findViewById(R.id.btnCheckOut);

        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new AttendanceAdapter(this, new ArrayList<>());
        rvHistory.setAdapter(historyAdapter);

        rvDepartment = findViewById(R.id.rvDepartment);
        rvDepartment.setLayoutManager(new LinearLayoutManager(this));
        departmentAdapter = new EmployeeStatsAdapter(this, new ArrayList<>());
        rvDepartment.setAdapter(departmentAdapter);

        // Hide Department Tab for non-managers
        if (!"MANAGER".equals(currentRole)) {
            tabDepartment.setVisibility(View.GONE);
        }
    }

    private void updateDateTime() {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("EEEE, dd 'tháng' MM, yyyy", new Locale("vi", "VN")).format(new Date());
        tvCurrentTime.setText(time);
        tvCurrentDate.setText(date);
    }

    private void setupTabs() {
        tabCheckIn.setOnClickListener(v -> switchTab(0));
        tabHistory.setOnClickListener(v -> switchTab(1));
        tabDepartment.setOnClickListener(v -> switchTab(2));
        switchTab(0); // Default
    }

    private void switchTab(int index) {
        // Reset looks
        tabCheckIn.setTextColor(Color.parseColor("#6B7280"));
        tabCheckIn.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabHistory.setTextColor(Color.parseColor("#6B7280"));
        tabHistory.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabDepartment.setTextColor(Color.parseColor("#6B7280"));
        tabDepartment.setTypeface(null, android.graphics.Typeface.NORMAL);

        layoutCheckIn.setVisibility(View.GONE);
        layoutHistory.setVisibility(View.GONE);
        layoutDepartment.setVisibility(View.GONE);

        if (index == 0) {
            tabCheckIn.setTextColor(Color.parseColor("#111827"));
            tabCheckIn.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutCheckIn.setVisibility(View.VISIBLE);
            fetchEmployeeStats(); // Refresh stats when returning to CheckIn tab
        } else if (index == 1) {
            tabHistory.setTextColor(Color.parseColor("#111827"));
            tabHistory.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutHistory.setVisibility(View.VISIBLE);
            fetchPersonalHistory();
        } else if (index == 2) {
            tabDepartment.setTextColor(Color.parseColor("#111827"));
            tabDepartment.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutDepartment.setVisibility(View.VISIBLE);
            fetchDepartmentStats();
        }
    }

    private void setupActionButtons() {
        btnCheckIn.setOnClickListener(v -> performCheckIn());
        btnCheckOut.setOnClickListener(v -> performCheckOut());
    }

    private void performCheckIn() {
        TimekeepingRequest req = new TimekeepingRequest(currentEmployeeId);
        apiService.checkIn(req).enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if(response.isSuccessful() && response.body() != null) {
                    Toast.makeText(TimekeepingActivity.this, "Check-in thành công!", Toast.LENGTH_SHORT).show();
                    tvCheckInTimeInfo.setText("Đã chấm công lúc: " + response.body().getCheckInTime());
                    btnCheckIn.setEnabled(false);
                    btnCheckIn.setAlpha(0.5f);
                    fetchEmployeeStats(); // Refresh stats
                } else {
                    Toast.makeText(TimekeepingActivity.this, "Lỗi check-in: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                Toast.makeText(TimekeepingActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performCheckOut() {
        TimekeepingRequest req = new TimekeepingRequest(currentEmployeeId);
        apiService.checkOut(req).enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if(response.isSuccessful() && response.body() != null) {
                    Toast.makeText(TimekeepingActivity.this, "Check-out thành công!", Toast.LENGTH_SHORT).show();
                    tvCheckOutTimeInfo.setText("Đã chấm ra lúc: " + response.body().getCheckOutTime());
                    btnCheckOut.setEnabled(false);
                    btnCheckOut.setAlpha(0.5f);
                } else {
                    Toast.makeText(TimekeepingActivity.this, "Lỗi check-out: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                Toast.makeText(TimekeepingActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchEmployeeStats() {
        apiService.getEmployeeAttendanceStats(currentEmployeeId, currentMonth, currentYear)
                .enqueue(new Callback<AttendanceStatsResponse>() {
            @Override
            public void onResponse(Call<AttendanceStatsResponse> call, Response<AttendanceStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AttendanceStatsResponse stats = response.body();
                    tvStatsOnTime.setText(String.valueOf(stats.getOnTime()));
                    tvStatsLate.setText(String.valueOf(stats.getLate()));
                    tvStatsAbsent.setText(String.valueOf(stats.getAbsent()));
                }
            }
            @Override
            public void onFailure(Call<AttendanceStatsResponse> call, Throwable t) { }
        });
    }

    private void fetchPersonalHistory() {
        apiService.getEmployeeAttendanceHistory(currentEmployeeId).enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historyAdapter.setAttendanceList(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Toast.makeText(TimekeepingActivity.this, "Lỗi tải lịch sử", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDepartmentStats() {
        apiService.getDepartmentAttendanceStats(currentDeptId, currentMonth, currentYear).enqueue(new Callback<List<EmployeeAttendanceStats>>() {
            @Override
            public void onResponse(Call<List<EmployeeAttendanceStats>> call, Response<List<EmployeeAttendanceStats>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    departmentAdapter.setStatsList(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<EmployeeAttendanceStats>> call, Throwable t) {
                Toast.makeText(TimekeepingActivity.this, "Lỗi tải dữ liệu phòng ban", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
