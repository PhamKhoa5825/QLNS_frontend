package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.adapter.AttendanceSummaryAdapter;
import com.example.myapplication.model.AttendanceMonthlyResponse;
import com.example.myapplication.model.AttendanceSummary;
import com.example.myapplication.model.SalaryRecord;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PayrollActivity extends AppCompatActivity {

    private TextView tvMonthYear, tvGrossSalary, tvGrade, tvScore;
    private TextView tvDaysWorked, tvLateInfo, tvAbsentExcused, tvDaysSick, tvAbsentUnexcused, tvOvertimeHours;
    private TextView tvBaseSalary, tvDeductionLate, tvDeductionUnexcused, tvDeductionSick, tvBonus, tvOvertimeBonus, tvGrossSalaryBreakdown;
    private TextView tvPayrollStatus, tvPayrollMainTitle;
    private View layoutDraftWarning;
    private TextView tvGrossSalaryLabel;
    private ImageButton btnPrevMonth, btnNextMonth;
    private TextView tvHeaderName, tvHeaderDept, tvHeaderAvatarText;
    private android.view.View containerProfileLink;

    private int currentMonth;
    private int currentYear;
    private Long employeeId;
    private String userRole;
    private RecyclerView rvCalendar;
    private AttendanceSummaryAdapter attendanceAdapter;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payroll);

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        employeeId = prefs.getEmployeeId();
        userRole = prefs.getRole();

        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1; // get(MONTH) is 0-indexed
        currentYear = cal.get(Calendar.YEAR);

        initViews();
        setupTopBar();
        updateMonthDisplay();
        fetchPayroll();
        fetchAttendanceSummary();

        BottomNavHelper.setupBottomNav(this, -1); // No nav item active for payroll screen
    }

    private void initViews() {
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvGrossSalary = findViewById(R.id.tvGrossSalary);
        tvGrade = findViewById(R.id.tvGrade);
        tvScore = findViewById(R.id.tvScore);
        tvDaysWorked = findViewById(R.id.tvDaysWorked);
        tvLateInfo = findViewById(R.id.tvLateInfo);
        tvAbsentExcused = findViewById(R.id.tvAbsentExcused);
        tvDaysSick = findViewById(R.id.tvDaysSick);
        tvAbsentUnexcused = findViewById(R.id.tvAbsentUnexcused);
        tvOvertimeHours = findViewById(R.id.tvOvertimeHours);
        tvBaseSalary = findViewById(R.id.tvBaseSalary);
        tvDeductionLate = findViewById(R.id.tvDeductionLate);
        tvDeductionUnexcused = findViewById(R.id.tvDeductionUnexcused);
        tvDeductionSick = findViewById(R.id.tvDeductionSick);
        tvBonus = findViewById(R.id.tvBonus);
        tvOvertimeBonus = findViewById(R.id.tvOvertimeBonus);
        tvGrossSalaryBreakdown = findViewById(R.id.tvGrossSalaryBreakdown);
        tvPayrollStatus = findViewById(R.id.tvPayrollStatus);
        tvPayrollMainTitle = findViewById(R.id.tvPayrollMainTitle);
        tvGrossSalaryLabel = findViewById(R.id.tvGrossSalaryLabel);
        layoutDraftWarning = findViewById(R.id.layoutDraftWarning);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        containerProfileLink = findViewById(R.id.containerProfileLink);
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderDept = findViewById(R.id.tvHeaderDept);
        tvHeaderAvatarText = findViewById(R.id.tvHeaderAvatarText);

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth--;
            if (currentMonth < 1) { currentMonth = 12; currentYear--; }
            updateMonthDisplay();
            fetchPayroll();
            fetchAttendanceSummary();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth++;
            if (currentMonth > 12) { currentMonth = 1; currentYear++; }
            updateMonthDisplay();
            fetchPayroll();
            fetchAttendanceSummary();
        });

        rvCalendar = findViewById(R.id.rvCalendar);
        rvCalendar.setLayoutManager(new GridLayoutManager(this, 7));
        attendanceAdapter = new AttendanceSummaryAdapter(this, new ArrayList<>());
        rvCalendar.setAdapter(attendanceAdapter);
    }

    private void setupTopBar() {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String name = prefs.getFullName();
        String dept = prefs.getDepartmentName();
        String username = prefs.getUsername();
        if (tvHeaderName != null) tvHeaderName.setText(name.isEmpty() ? username : name);
        if (tvHeaderDept != null) tvHeaderDept.setText(dept != null ? dept : "No Department");
        if (tvHeaderAvatarText != null && !username.isEmpty())
            tvHeaderAvatarText.setText(String.valueOf(username.charAt(0)).toUpperCase());
        if (containerProfileLink != null)
            containerProfileLink.setOnClickListener(v -> startActivity(new android.content.Intent(this, ProfileActivity.class)));
    }

    private void updateMonthDisplay() {
        tvMonthYear.setText(String.format("Tháng %02d / %d", currentMonth, currentYear));
        
        // Update Title: If current month/year, show "Bảng lương tính tới hiện tại"
        Calendar now = Calendar.getInstance();
        int monthNow = now.get(Calendar.MONTH) + 1;
        int yearNow = now.get(Calendar.YEAR);
        
        if (tvPayrollMainTitle != null) {
            if (currentMonth == monthNow && currentYear == yearNow) {
                tvPayrollMainTitle.setText("Bảng lương tính tới hiện tại");
            } else {
                tvPayrollMainTitle.setText("Bảng Lương");
            }
        }
    }

    private void fetchAttendanceSummary() {
        Long targetId = getIntent().getLongExtra("employeeId", -1L);
        if (targetId == -1L) targetId = employeeId;

        ApiService api = RetrofitClient.getApiService(this);
        api.getAttendanceSummary(targetId, currentMonth, currentYear).enqueue(new Callback<AttendanceMonthlyResponse>() {
            @Override
            public void onResponse(Call<AttendanceMonthlyResponse> call, Response<AttendanceMonthlyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AttendanceSummary> days = response.body().getDays();
                    android.util.Log.d("PAYROLL_GRID", "API success, days count: " + (days != null ? days.size() : "null"));
                    if (days != null && !days.isEmpty()) {
                        updateCalendarGrid(days);
                    } else {
                        android.util.Log.w("PAYROLL_GRID", "Days list is null or empty");
                    }
                } else {
                    String errorMsg = "Grid API error: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) { /* ignore */ }
                    android.util.Log.e("PAYROLL_GRID", errorMsg);
                    Toast.makeText(PayrollActivity.this, "Lỗi grid: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<AttendanceMonthlyResponse> call, Throwable t) {
                android.util.Log.e("PAYROLL_GRID", "Network failure: " + t.getMessage(), t);
                Toast.makeText(PayrollActivity.this, "Grid lỗi: " + t.getMessage(), Toast.LENGTH_LONG).show();
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

    private void fetchPayroll() {
        // If coming from Admin view, we might have an employeeId from extras
        Long targetId = getIntent().getLongExtra("employeeId", -1L);
        if (targetId == -1L) targetId = employeeId;
        
        if (getIntent().hasExtra("month")) {
             currentMonth = getIntent().getIntExtra("month", currentMonth);
             currentYear = getIntent().getIntExtra("year", currentYear);
        }

        ApiService api = RetrofitClient.getApiService(this);
        api.getMyPayroll(targetId, currentMonth, currentYear).enqueue(new Callback<SalaryRecord>() {
            @Override
            public void onResponse(Call<SalaryRecord> call, Response<SalaryRecord> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindData(response.body());
                } else if (response.code() == 404) {
                    showEmpty();
                } else {
                    String msg = "Chưa có bảng lương tháng này";
                    if (response.errorBody() != null) {
                        try {
                            String errorJson = response.errorBody().string();
                            com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(errorJson).getAsJsonObject();
                            if (obj.has("message")) {
                                msg = obj.get("message").getAsString();
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    Toast.makeText(PayrollActivity.this, msg + " (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    showEmpty();
                }
            }
            @Override
            public void onFailure(Call<SalaryRecord> call, Throwable t) {
                Toast.makeText(PayrollActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void finalizeRecord(Long recordId) {
        if (recordId == null) return;
        ApiService api = RetrofitClient.getApiService(this);
        api.finalizePayroll(recordId).enqueue(new Callback<SalaryRecord>() {
            @Override
            public void onResponse(Call<SalaryRecord> call, Response<SalaryRecord> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PayrollActivity.this, "✅ Đã chốt lương thành công!", Toast.LENGTH_SHORT).show();
                    bindData(response.body());
                } else {
                    Toast.makeText(PayrollActivity.this, "Lỗi khi chốt lương", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<SalaryRecord> call, Throwable t) {
                Toast.makeText(PayrollActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindData(SalaryRecord s) {
        String gross = formatCurrency(s.getGrossSalary());
        tvGrossSalary.setText(gross);
        tvGrossSalaryBreakdown.setText(gross);

        tvGrade.setText("Xếp loại: " + s.getPerformanceGrade());
        tvScore.setText(String.format("Điểm: %.0f/100", s.getPerformanceScore()));

        int standard = s.getWorkingDaysStandard() != null ? s.getWorkingDaysStandard() : 22;
        double worked = s.getDaysWorked() != null ? s.getDaysWorked() : 0.0;
        double trip = s.getBusinessTripDays() != null ? s.getBusinessTripDays() : 0.0;
        double office = Math.max(0, worked - trip);
        tvDaysWorked.setText(String.format("(%s + %s) / %d ngày", formatDays(office), formatDays(trip), standard));
        tvLateInfo.setText((s.getTotalLateMinutes() != null ? s.getTotalLateMinutes() : 0) + " phút");
        tvAbsentExcused.setText(formatDays(s.getDaysAbsentExcused() != null ? s.getDaysAbsentExcused() : 0.0) + " ngày");
        tvDaysSick.setText(formatDays(s.getDaysSick() != null ? s.getDaysSick() : 0.0) + " ngày");
        tvAbsentUnexcused.setText(formatDays(s.getDaysAbsentUnexcused() != null ? s.getDaysAbsentUnexcused() : 0.0) + " ngày");
        tvOvertimeHours.setText((s.getTotalOvertimeHours() != null ? s.getTotalOvertimeHours() : 0) + " giờ");

        tvBaseSalary.setText(formatCurrency(s.getBaseSalary()));
        tvDeductionLate.setText("- " + formatCurrency(s.getDeductionLate()));
        tvDeductionUnexcused.setText("- " + formatCurrency(s.getDeductionUnexcused()));
        tvDeductionSick.setText("- " + formatCurrency(s.getDeductionSick()));
        tvBonus.setText("+ " + formatCurrency(s.getTaskBonus()));
        tvOvertimeBonus.setText("+ " + formatCurrency(s.getOvertimeBonus()));

        boolean isFinalized = "FINALIZED".equals(s.getStatus());
        boolean isDraft = "DRAFT".equals(s.getStatus());
        
        // Show/Hide Finalize button for Admin
        boolean isAdminView = getIntent().getBooleanExtra("isAdminView", false);
        com.google.android.material.button.MaterialButton btnFinalize = findViewById(R.id.btnFinalize);
        if (isAdminView && isDraft && "ADMIN".equals(userRole)) {
            btnFinalize.setVisibility(View.VISIBLE);
            btnFinalize.setOnClickListener(v -> finalizeRecord(s.getId()));
        } else {
            btnFinalize.setVisibility(View.GONE);
        }

        if (isFinalized) {
            tvGrossSalary.setText(gross);
            tvGrossSalaryLabel.setText("Thực Nhận");
            findViewById(R.id.tvBreakdownLabel).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvBreakdownLabel)).setText("Thực nhận");
            layoutDraftWarning.setVisibility(View.GONE);
            tvPayrollStatus.setText("✅ ĐÃ CHỐT");
            tvPayrollStatus.setTextColor(android.graphics.Color.parseColor("#10B981"));
        } else {
            tvGrossSalary.setText("~ " + gross);
            tvGrossSalaryLabel.setText("Lương Ước Tính");
            ((TextView) findViewById(R.id.tvBreakdownLabel)).setText("Ước nhận");
            layoutDraftWarning.setVisibility(View.VISIBLE);
            tvPayrollStatus.setText("🕐 ƯỚC TÍNH");
            tvPayrollStatus.setTextColor(android.graphics.Color.parseColor("#F97316"));
        }
    }

    private void showEmpty() {
        tvGrossSalary.setText("Chưa có dữ liệu");
        tvGrossSalaryBreakdown.setText("-- ₫");
        tvGrade.setText("Xếp loại: --");
        tvScore.setText("Điểm: --/100");
        tvDaysWorked.setText("-- ngày");
        tvLateInfo.setText("-- phút");
        tvAbsentExcused.setText("-- ngày");
        if (tvDaysSick != null) tvDaysSick.setText("-- ngày");
        tvAbsentUnexcused.setText("-- ngày");
        tvOvertimeHours.setText("-- giờ");
        tvBaseSalary.setText("-- ₫");
        tvDeductionLate.setText("-- ₫");
        tvDeductionUnexcused.setText("-- ₫");
        if (tvDeductionSick != null) tvDeductionSick.setText("-- ₫");
        tvBonus.setText("-- ₫");
        tvOvertimeBonus.setText("-- ₫");
        tvPayrollStatus.setText("Chưa tạo");
    }

    private String formatCurrency(Double amount) {
        if (amount == null) return "0 ₫";
        return CURRENCY_FORMAT.format(amount.longValue()) + " ₫";
    }

    private String formatDays(Double days) {
        if (days == null) return "0";
        if (days == days.intValue()) {
            return String.valueOf(days.intValue());
        }
        return String.format(Locale.US, "%.1f", days);
    }
}
