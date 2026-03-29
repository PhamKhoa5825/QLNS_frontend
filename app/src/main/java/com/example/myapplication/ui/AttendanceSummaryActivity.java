package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.AttendanceSummaryAdapter;
import com.example.myapplication.model.AttendanceSummary;
import com.example.myapplication.model.AttendanceMonthlyResponse;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AttendanceSummaryActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView tvCurrentMonthRange;
    private TextView tvTotalWorkingDays, tvDaysPresent, tvDaysLeave, tvTotalLateMinutes;
    private TextView tvBaseSalary, tvBonusTotal, tvDeductionTotal, tvEstimatedNet;
    
    private RecyclerView rvCalendar;
    private AttendanceSummaryAdapter adapter;
    
    private int currentMonth, currentYear;
    private Long employeeId;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_summary);

        employeeId = SharedPrefsManager.getInstance(this).getEmployeeId();
        apiService = RetrofitClient.getApiService(this);

        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear = cal.get(Calendar.YEAR);

        initViews();
        fetchSummary();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackSummary);
        btnBack.setOnClickListener(v -> finish());

        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        tvCurrentMonthRange = findViewById(R.id.tvCurrentMonthRange);

        tvTotalWorkingDays = findViewById(R.id.tvTotalWorkingDays);
        tvDaysPresent = findViewById(R.id.tvDaysPresent);
        tvDaysLeave = findViewById(R.id.tvDaysLeave);
        tvTotalLateMinutes = findViewById(R.id.tvTotalLateMinutes);

        tvBaseSalary = findViewById(R.id.tvBaseSalary);
        tvBonusTotal = findViewById(R.id.tvBonusTotal);
        tvDeductionTotal = findViewById(R.id.tvDeductionTotal);
        tvEstimatedNet = findViewById(R.id.tvEstimatedNet);

        rvCalendar = findViewById(R.id.rvCalendar);
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        adapter = new AttendanceSummaryAdapter(this, new ArrayList<>());
        rvCalendar.setAdapter(adapter);

        btnPrevMonth.setOnClickListener(v -> {
            if (currentMonth == 1) {
                currentMonth = 12;
                currentYear--;
            } else {
                currentMonth--;
            }
            fetchSummary();
        });

        btnNextMonth.setOnClickListener(v -> {
            if (currentMonth == 12) {
                currentMonth = 1;
                currentYear++;
            } else {
                currentMonth++;
            }
            fetchSummary();
        });
    }

    private void fetchSummary() {
        tvCurrentMonthRange.setText(String.format("Tháng %02d, %d", currentMonth, currentYear));
        
        apiService.getAttendanceSummary(employeeId, currentMonth, currentYear).enqueue(new Callback<AttendanceMonthlyResponse>() {
            @Override
            public void onResponse(Call<AttendanceMonthlyResponse> call, Response<AttendanceMonthlyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AttendanceMonthlyResponse data = response.body();
                    updateCalendarGrid(data.getDays());
                    calculateStatistics(data.getDays(), data.getStandardWorkingDays());
                    updateSalaryInfo(data);
                } else {
                    Toast.makeText(AttendanceSummaryActivity.this, "Lỗi tải dữ liệu: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AttendanceMonthlyResponse> call, Throwable t) {
                Toast.makeText(AttendanceSummaryActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSalaryInfo(AttendanceMonthlyResponse data) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
        
        tvBaseSalary.setText(df.format(data.getBaseSalary()) + " VNĐ");
        tvBonusTotal.setText("+" + df.format(data.getTotalBonuses()) + " VNĐ");
        tvDeductionTotal.setText("-" + df.format(data.getTotalDeductions()) + " VNĐ");
        tvEstimatedNet.setText(df.format(data.getEstimatedSalarySoFar()) + " VNĐ");
    }

    private void updateCalendarGrid(List<AttendanceSummary> summaryList) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, currentYear);
        cal.set(Calendar.MONTH, currentMonth - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun, 2=Mon...
        // Map to grid (Mon=0, Tue=1, ..., Sat=5, Sun=6)
        int emptySlots = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : (firstDayOfWeek - 2);

        List<AttendanceSummary> displayList = new ArrayList<>();
        for (int i = 0; i < emptySlots; i++) {
            displayList.add(null);
        }
        displayList.addAll(summaryList);
        
        adapter.setSummaryList(displayList);
    }

    private void calculateStatistics(List<AttendanceSummary> summaryList, Integer standardDays) {
        double present = 0;
        double leave = 0;
        int lateMinutes = 0;
        
        for (AttendanceSummary s : summaryList) {
            if (s == null) continue;
            String status = s.getStatus();
            double value = s.getDayValue() != null ? s.getDayValue() : 1.0;
            
            if ("PRESENT".equals(status) || "LATE".equals(status) || "OT".equals(status) || "TRIP".equals(status)) {
                present += value;
            }
            if ("LEAVE".equals(status) || "LEAVE_PARTIAL".equals(status)
                    || "LEAVE_ANNUAL".equals(status) || "LEAVE_ANNUAL_PARTIAL".equals(status)
                    || "LEAVE_UNPAID".equals(status) || "LEAVE_UNPAID_PARTIAL".equals(status)) {
                leave += value;
            }
            
            if (s.getDescription() != null && s.getDescription().contains("Trễ")) {
                try {
                    String desc = s.getDescription();
                    int start = desc.indexOf("Trễ") + 4;
                    int end = desc.indexOf("m", start);
                    if (end > start) {
                        lateMinutes += Integer.parseInt(desc.substring(start, end).trim());
                    }
                } catch (Exception ignored) {}
            }
        }
        
        tvDaysPresent.setText(String.valueOf(present));
        tvDaysLeave.setText(String.valueOf(leave));
        tvTotalLateMinutes.setText(lateMinutes + "m");
        tvTotalWorkingDays.setText(String.valueOf(standardDays != null ? standardDays : 22));
    }
}
