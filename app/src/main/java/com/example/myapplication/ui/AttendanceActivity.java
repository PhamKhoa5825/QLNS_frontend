package com.example.myapplication.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.AttendanceSummaryAdapter;
import com.example.myapplication.model.AttendanceMonthlyResponse;
import com.example.myapplication.model.AttendanceSummary;
import com.example.myapplication.model.SalaryRecord;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.TopBarHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceActivity extends AppCompatActivity {

    private TextView tvMonthYear;
    private TextView tvDaysWorked, tvLateInfo, tvAbsentExcused, tvDaysSick, tvAbsentUnexcused, tvOvertimeHours;
    private ImageButton btnPrevMonth, btnNextMonth;
    
    private int currentMonth;
    private int currentYear;
    private Long employeeId;
    private RecyclerView rvCalendar;
    private AttendanceSummaryAdapter attendanceAdapter;
    
    private double onTimeCount = 0, lateCount = 0, tripCount = 0;
    private SalaryRecord currentSalaryRecord;
    private List<AttendanceSummary> currentSummaryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        employeeId = prefs.getEmployeeId();

        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);

        initViews();
        updateMonthDisplay();
        fetchData();

        BottomNavHelper.setupBottomNav(this, -1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TopBarHelper.setupTopBar(this);
    }

    private void initViews() {
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvDaysWorked = findViewById(R.id.tvDaysWorked);
        tvLateInfo = findViewById(R.id.tvLateInfo);
        tvAbsentExcused = findViewById(R.id.tvAbsentExcused);
        tvDaysSick = findViewById(R.id.tvDaysSick);
        tvAbsentUnexcused = findViewById(R.id.tvAbsentUnexcused);
        tvOvertimeHours = findViewById(R.id.tvOvertimeHours);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 1) { currentMonth = 12; currentYear--; }
            updateMonthDisplay();
            fetchData();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 12) { currentMonth = 1; currentYear++; }
            updateMonthDisplay();
            fetchData();
        });

        rvCalendar = findViewById(R.id.rvCalendar);
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        attendanceAdapter = new AttendanceSummaryAdapter(this, new ArrayList<>());
        rvCalendar.setAdapter(attendanceAdapter);
    }

    private void updateMonthDisplay() {
        tvMonthYear.setText(String.format("Tháng %02d / %d", currentMonth, currentYear));
    }

    private void fetchData() {
        fetchAttendanceSummary();
        fetchAttendanceStats();
    }

    private void fetchAttendanceSummary() {
        ApiService api = RetrofitClient.getApiService(this);
        api.getAttendanceSummary(employeeId, currentMonth, currentYear).enqueue(new Callback<AttendanceMonthlyResponse>() {
            @Override
            public void onResponse(Call<AttendanceMonthlyResponse> call, Response<AttendanceMonthlyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentSummaryList = response.body().getDays();
                    if (currentSummaryList != null) {
                        calculateCounts(currentSummaryList);
                        updateCalendarGrid(currentSummaryList);
                        refreshStatsUI();
                    }
                } else {
                    Toast.makeText(AttendanceActivity.this, "Lỗi tải lịch sử: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<AttendanceMonthlyResponse> call, Throwable t) {
                Toast.makeText(AttendanceActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAttendanceStats() {
        ApiService api = RetrofitClient.getApiService(this);
        api.getMyPayroll(employeeId, currentMonth, currentYear).enqueue(new Callback<SalaryRecord>() {
            @Override
            public void onResponse(Call<SalaryRecord> call, Response<SalaryRecord> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentSalaryRecord = response.body();
                    refreshStatsUI();
                } else {
                    showEmptyStats();
                }
            }
            @Override
            public void onFailure(Call<SalaryRecord> call, Throwable t) {
                showEmptyStats();
            }
        });
    }

    private void updateCalendarGrid(List<AttendanceSummary> summaryList) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, currentYear);
        cal.set(Calendar.MONTH, currentMonth - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); 
        int emptySlots = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : (firstDayOfWeek - 2);

        List<AttendanceSummary> displayList = new ArrayList<>();
        for (int i = 0; i < emptySlots; i++) {
            displayList.add(null);
        }
        displayList.addAll(summaryList);
        attendanceAdapter.setSummaryList(displayList);
    }

    private void calculateCounts(List<AttendanceSummary> list) {
        onTimeCount = 0;
        lateCount = 0;
        tripCount = 0;
        for (AttendanceSummary s : list) {
            String status = s.getStatus();
            double val = s.getDayValue() != null ? s.getDayValue() : 0.0;
            if ("PRESENT".equals(status)) onTimeCount += 1.0;
            else if ("PRESENT_PARTIAL".equals(status)) onTimeCount += 0.5;
            else if ("LATE".equals(status)) lateCount += 1.0;
            else if ("LATE_PARTIAL".equals(status)) lateCount += 0.5;
            else if ("TRIP".equals(status)) tripCount += val; // TRIP usually counts as 1.0 in this app
        }
    }

    private void refreshStatsUI() {
        if (currentSalaryRecord == null) return;
        bindStats(currentSalaryRecord);
    }

    private void bindStats(SalaryRecord s) {
        int standard = s.getWorkingDaysStandard() != null ? s.getWorkingDaysStandard() : 22;
        
        // Use SpannableStringBuilder for multi-color formatting
        android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder();
        builder.append("(");
        
        // On-time (Green)
        int start = builder.length();
        builder.append(formatDays(onTimeCount));
        builder.setSpan(new android.text.style.ForegroundColorSpan(0xFF4CAF50), start, builder.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        builder.append(" + ");
        
        // Late (Red)
        start = builder.length();
        builder.append(formatDays(lateCount));
        builder.setSpan(new android.text.style.ForegroundColorSpan(0xFFF44336), start, builder.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        builder.append(" + ");
        
        // Trip (Blue)
        start = builder.length();
        builder.append(formatDays(tripCount));
        builder.setSpan(new android.text.style.ForegroundColorSpan(0xFF2196F3), start, builder.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        builder.append(") / ");
        
        // Standard (Green)
        start = builder.length();
        builder.append(String.valueOf(standard));
        builder.setSpan(new android.text.style.ForegroundColorSpan(0xFF4CAF50), start, builder.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        builder.append(" ngày");
        
        tvDaysWorked.setText(builder);

        tvLateInfo.setText((s.getTotalLateMinutes() != null ? s.getTotalLateMinutes() : 0) + " phút");
        tvAbsentExcused.setText(formatDays(s.getDaysAbsentExcused() != null ? s.getDaysAbsentExcused() : 0.0) + " ngày");
        tvDaysSick.setText(formatDays(s.getDaysSick() != null ? s.getDaysSick() : 0.0) + " ngày");
        tvAbsentUnexcused.setText(formatDays(s.getDaysAbsentUnexcused() != null ? s.getDaysAbsentUnexcused() : 0.0) + " ngày");
        tvOvertimeHours.setText((s.getTotalOvertimeHours() != null ? s.getTotalOvertimeHours() : 0) + " giờ");
    }

    private void showEmptyStats() {
        tvDaysWorked.setText("-- / -- ngày");
        tvLateInfo.setText("-- phút");
        tvAbsentExcused.setText("-- ngày");
        tvDaysSick.setText("-- ngày");
        tvAbsentUnexcused.setText("-- ngày");
        tvOvertimeHours.setText("-- giờ");
    }

    private String formatDays(Double days) {
        if (days == null) return "0";
        if (days == days.intValue()) {
            return String.valueOf(days.intValue());
        }
        return String.format(Locale.US, "%.1f", days);
    }
}
