package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.myapplication.R;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.RequestModels;
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

    private TextView tvCurrentDate, tvCurrentTime;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Status bar trong suốt, header gradient chạy lên phía sau ──
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_dashboard);

        // Đẩy avatar xuống bằng chiều cao status bar (tránh bị che)
        TextView tvAvatar = findViewById(R.id.tvAvatar);
        ViewCompat.setOnApplyWindowInsetsListener(tvAvatar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int dp24 = (int) (24 * getResources().getDisplayMetrics().density);
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) v.getLayoutParams();
            params.topMargin = dp24 + statusBarHeight;
            v.setLayoutParams(params);
            return insets;
        });

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

    private void setupHeader() {
        String fullName = prefs.getString("fullName", "Người dùng");
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvAvatar   = findViewById(R.id.tvAvatar);
        tvUserName.setText(fullName);
        if (fullName != null && !fullName.isEmpty()) {
            String trimmed = fullName.trim();
            if (!trimmed.isEmpty()) {
                String[] parts = trimmed.split(" ");
                String lastWord = parts[parts.length - 1];
                tvAvatar.setText(!lastWord.isEmpty()
                        ? String.valueOf(lastWord.charAt(0)).toUpperCase() : "?");
            } else {
                tvAvatar.setText("?");
            }
        }

    }

    private void setupDateTime() {
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        timeHandler.post(new Runnable() {
            @Override public void run() {
                Date now = new Date();
                String date = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi")).format(now);
                if (tvCurrentDate != null)
                    tvCurrentDate.setText(date.substring(0, 1).toUpperCase() + date.substring(1));
                if (tvCurrentTime != null)
                    tvCurrentTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
                timeHandler.postDelayed(this, 30_000);
            }
        });
    }

    private void setupStatsCards() {
        safeClick(R.id.cardStatEmployee, () -> startActivity(new Intent(this, EmployeeActivity.class)));
        safeClick(R.id.cardStatDept,     () -> startActivity(new Intent(this, DepartmentActivity.class)));
        safeClick(R.id.cardStatRequest,  () -> startActivity(new Intent(this, RequestListActivity.class)));
        safeClick(R.id.cardStatTask,     () -> startActivity(new Intent(this, TaskActivity.class)));
    }

    private void setupNavigation() {
        safeClick(R.id.btnNavEmployee,     () -> startActivity(new Intent(this, EmployeeActivity.class)));
        safeClick(R.id.btnNavDepartment,   () -> startActivity(new Intent(this, DepartmentActivity.class)));
        safeClick(R.id.btnNavNotification, () -> startActivity(new Intent(this, NotificationCenterActivity.class)));

        safeClick(R.id.btnNavRequest, () -> startActivity(new Intent(this, RequestListActivity.class)));
        safeClick(R.id.btnNavAccount, () -> {
            if ("ADMIN".equals(role)) startActivity(new Intent(this, AccountManagementActivity.class));
            else startActivity(new Intent(this, ProfileActivity.class));
        });

        safeClick(R.id.btnNavLogout, this::showLogoutDialog);

        if ("ADMIN".equals(role)) {
            View btnSettings = findViewById(R.id.btnNavSettings);
            if (btnSettings != null) btnSettings.setVisibility(View.VISIBLE);
            safeClick(R.id.btnNavSettings, () -> startActivity(new Intent(this, CompanySettingsActivity.class)));

            View layoutMenuAdmin = findViewById(R.id.layoutMenuAdmin);
            if (layoutMenuAdmin != null) layoutMenuAdmin.setVisibility(View.VISIBLE);
            safeClick(R.id.btnNavSystemLog, () -> startActivity(new Intent(this, SystemLogActivity.class)));
        }
    }

    private void loadStats() {
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    updateStat(R.id.tvStatEmployee, String.valueOf(r.body().size()));
                } else if (r.code() == 401) {
                    handleSessionExpired();
                }
            }
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) {
                updateStat(R.id.tvStatEmployee, "!");
            }
        });

        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override public void onResponse(Call<List<Department>> c, Response<List<Department>> r) {
                if (r.isSuccessful() && r.body() != null)
                    updateStat(R.id.tvStatDept, String.valueOf(r.body().size()));
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) {
                updateStat(R.id.tvStatDept, "!");
            }
        });

        Call<List<RequestModels.RequestResponse>> requestCall;
        if ("ADMIN".equals(role)) {
            requestCall = apiService.getAllRequests();
        } else {
            requestCall = apiService.getMyRequests(employeeId);
        }
        requestCall.enqueue(new Callback<List<RequestModels.RequestResponse>>() {
            @Override public void onResponse(Call<List<RequestModels.RequestResponse>> c,
                                             Response<List<RequestModels.RequestResponse>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    long pending = r.body().stream()
                            .filter(req -> "PENDING".equals(req.status))
                            .count();
                    updateStat(R.id.tvStatRequest, String.valueOf(pending));
                }
            }
            @Override public void onFailure(Call<List<RequestModels.RequestResponse>> c, Throwable t) {
                updateStat(R.id.tvStatRequest, "!");
            }
        });

        apiService.getMyTasks(employeeId).enqueue(new Callback<List<TaskModels.TaskResponse>>() {
            @Override public void onResponse(Call<List<TaskModels.TaskResponse>> c,
                                             Response<List<TaskModels.TaskResponse>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    long pending = r.body().stream()
                            .filter(t -> !"DONE".equals(t.status))
                            .count();
                    updateStat(R.id.tvStatTask, String.valueOf(pending));
                }
            }
            @Override public void onFailure(Call<List<TaskModels.TaskResponse>> c, Throwable t) {
                updateStat(R.id.tvStatTask, "!");
            }
        });
    }

    private void handleSessionExpired() {
        Toast.makeText(this, "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
        String savedUsername = prefs.getString("saved_username", null);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        if (savedUsername != null) editor.putString("saved_username", savedUsername);
        editor.apply();
        startActivity(new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

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
            String savedEmail = prefs.getString("saved_username", null);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            if (savedEmail != null) editor.putString("saved_username", savedEmail);
            editor.apply();
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
        dialog.show();
    }

    private void updateStat(int id, String val) {
        try { TextView tv = findViewById(id); if (tv != null) tv.setText(val); }
        catch (Exception ignored) {}
    }

    private void safeClick(int id, Runnable action) {
        View v = findViewById(id);
        if (v != null) {
            v.setOnClickListener(view -> action.run());
            v.setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });
        }
    }

    @Override protected void onResume() { super.onResume(); loadStats(); }
    @Override protected void onDestroy() { super.onDestroy(); timeHandler.removeCallbacksAndMessages(null); }
}