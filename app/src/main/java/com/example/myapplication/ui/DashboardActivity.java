package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.AttendanceModels;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.TaskModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private ApiService apiService;
    private String role;
    private Long employeeId;

    // Date & time
    private TextView tvCurrentDate, tvCurrentTime;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        role       = prefs.getString("role", "EMPLOYEE");
        employeeId = prefs.getLong("employeeId", -1);

        setupHeader();
        setupDateTime();
        setupStatsCards();
        setupNavigation();
        loadStats();
    }

    // ── HEADER ────────────────────────────────────────────────────

    private void setupHeader() {
        String fullName = prefs.getString("fullName", "Admin");
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvAvatar   = findViewById(R.id.tvAvatar);

        tvUserName.setText(fullName);
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.trim().split(" ");
            tvAvatar.setText(String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase());
        }
    }

    // ── NGÀY GIỜ THỰC (layoutDate) ───────────────────────────────

    private void setupDateTime() {
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);

        timeHandler.post(new Runnable() {
            @Override
            public void run() {
                Date now = new Date();

                // Ngày: "Thứ 2, 16/03/2026"
                String date = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi")).format(now);
                String capDate = date.substring(0, 1).toUpperCase() + date.substring(1);
                if (tvCurrentDate != null) tvCurrentDate.setText(capDate);

                // Giờ: "14:30"
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now);
                if (tvCurrentTime != null) tvCurrentTime.setText(time);

                timeHandler.postDelayed(this, 30_000); // cập nhật mỗi 30s
            }
        });
    }

    // ── STATS CARDS (clickable) ───────────────────────────────────

    private void setupStatsCards() {
        // Click vào card → mở trang tương ứng
        safeClick(R.id.cardStatEmployee, () ->
                startActivity(new Intent(this, EmployeeActivity.class)));

        safeClick(R.id.cardStatDept, () ->
                startActivity(new Intent(this, DepartmentActivity.class)));

        safeClick(R.id.cardStatAttendance, () ->
                startActivity(new Intent(this, TimekeepingActivity.class)));

        safeClick(R.id.cardStatTask, () ->
                startActivity(new Intent(this, TaskActivity.class)));
    }

    // ── QUICK ACCESS NAVIGATION ───────────────────────────────────

    private void setupNavigation() {
        safeClick(R.id.btnNavEmployee,     () -> startActivity(new Intent(this, EmployeeActivity.class)));
        safeClick(R.id.btnNavDepartment,   () -> startActivity(new Intent(this, DepartmentActivity.class)));
        // XÓA: btnNavTimekeeping không còn trong XML
        safeClick(R.id.btnNavChat,         () -> startActivity(new Intent(this, ChatListActivity.class)));
        safeClick(R.id.btnNavTask,         () -> startActivity(new Intent(this, TaskActivity.class)));
        safeClick(R.id.btnNavNotification, () -> startActivity(new Intent(this, NotificationCenterActivity.class)));

        safeClick(R.id.btnNavAccount, () -> {
            if ("ADMIN".equals(role)) {
                startActivity(new Intent(this, AccountManagementActivity.class));
            } else {
                startActivity(new Intent(this, ProfileActivity.class));
            }
        });

        safeClick(R.id.btnNavLogout, this::showLogoutDialog);
    }

    // ── LOAD THỐNG KÊ ─────────────────────────────────────────────

    private void loadStats() {
        // 1. Tổng nhân viên
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    updateStat(R.id.tvStatEmployee, String.valueOf(r.body().size()));
                }
            }
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) {}
        });

        // 2. Tổng phòng ban
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override public void onResponse(Call<List<Department>> c, Response<List<Department>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    updateStat(R.id.tvStatDept, String.valueOf(r.body().size()));
                }
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) {}
        });

        // 3. Chấm công hôm nay (số người đã check-in)
        apiService.getTodayAttendance().enqueue(new Callback<List<AttendanceModels.AttendanceResponse>>() {
            @Override public void onResponse(Call<List<AttendanceModels.AttendanceResponse>> c,
                                             Response<List<AttendanceModels.AttendanceResponse>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    updateStat(R.id.tvStatAttendance, String.valueOf(r.body().size()));
                }
            }
            @Override public void onFailure(Call<List<AttendanceModels.AttendanceResponse>> c, Throwable t) {
                updateStat(R.id.tvStatAttendance, "0");
            }
        });

        // 4. Nhiệm vụ của tôi
        apiService.getMyTasks(employeeId).enqueue(new Callback<List<TaskModels.TaskResponse>>() {
            @Override public void onResponse(Call<List<TaskModels.TaskResponse>> c,
                                             Response<List<TaskModels.TaskResponse>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    // Đếm task chưa hoàn thành
                    long pending = r.body().stream()
                            .filter(t -> !"DONE".equals(t.status))
                            .count();
                    updateStat(R.id.tvStatTask, String.valueOf(pending));
                }
            }
            @Override public void onFailure(Call<List<TaskModels.TaskResponse>> c, Throwable t) {
                updateStat(R.id.tvStatTask, "0");
            }
        });
    }

    // ── LOGOUT ────────────────────────────────────────────────────

    private void showLogoutDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirmation);

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView btnClose      = dialog.findViewById(R.id.btnCloseDialog);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmLogout);
        MaterialButton btnCancel  = dialog.findViewById(R.id.btnCancelLogout);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnConfirm != null) btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            doLogout();
        });
        dialog.show();
    }

    private void doLogout() {
        prefs.edit().clear().apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ── UTILS ─────────────────────────────────────────────────────

    private void updateStat(int viewId, String value) {
        try {
            TextView tv = findViewById(viewId);
            if (tv != null) tv.setText(value);
        } catch (Exception ignored) {}
    }

    private void safeClick(int viewId, Runnable action) {
        try {
            View v = findViewById(viewId);
            if (v != null) v.setOnClickListener(view -> action.run());
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats(); // refresh khi quay lại
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeHandler.removeCallbacksAndMessages(null);
    }
}