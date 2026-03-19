package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.myapplication.R;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.AdminModels;
import com.example.myapplication.model.RequestModels;
import com.example.myapplication.model.TaskModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private Long userId;

    private TextView tvCurrentDate, tvCurrentTime, tvStatsPageInfo;
    private ViewPager2 vpStats;
    private LinearLayout layoutDots;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());

    // ── Stat data model ───────────────────────────────────────────
    static class StatItem {
        int iconRes; int iconTint; String label; String value = "--"; Runnable onClick;
        StatItem(int iconRes, int iconTint, String label, Runnable onClick) {
            this.iconRes = iconRes; this.iconTint = iconTint;
            this.label = label; this.onClick = onClick;
        }
    }

    private final List<StatItem> allStats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_dashboard);

        // Đẩy avatar xuống tránh status bar
        TextView tvAvatar = findViewById(R.id.tvAvatar);
        ViewCompat.setOnApplyWindowInsetsListener(tvAvatar, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int dp24 = (int) (24 * getResources().getDisplayMetrics().density);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            params.topMargin = dp24 + statusBarHeight;
            v.setLayoutParams(params);
            return insets;
        });

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        role       = prefs.getString("role", "EMPLOYEE");
        employeeId = prefs.getLong("employeeId", -1);
        userId     = prefs.getLong("userId", -1);

        setupHeader();
        setupDateTime();
        buildStatItems();
        setupStatsViewPager();
        setupNavigation();
        setupBottomNav();
        loadStats();
    }

    // ── HEADER ────────────────────────────────────────────────────

    private void setupHeader() {
        String fullName = prefs.getString("fullName", "Người dùng");
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvAvatar   = findViewById(R.id.tvAvatar);
        tvUserName.setText(fullName);
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.trim().split(" ");
            String lastWord = parts[parts.length - 1];
            tvAvatar.setText(!lastWord.isEmpty() ? String.valueOf(lastWord.charAt(0)).toUpperCase() : "?");
        } else {
            tvAvatar.setText("?");
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

    // ── STAT ITEMS ────────────────────────────────────────────────

    // Page 1: 4 stat nhỏ (index 0-3)
    // Page 2: Company settings full (dữ liệu riêng, không dùng allStats)
    // Page 3: 4 stat nhỏ (index 4-7)

    private void buildStatItems() {
        allStats.clear();

        // Page 1 (index 0-3)
        allStats.add(new StatItem(R.drawable.outline_contacts_product_24,
                ContextCompat.getColor(this, R.color.colorPrimary),
                "Tổng nhân viên", () -> startActivity(new Intent(this, EmployeeActivity.class))));
        allStats.add(new StatItem(R.drawable.company, 0xFFA855F7,
                "Phòng ban", () -> startActivity(new Intent(this, DepartmentActivity.class))));
        allStats.add(new StatItem(android.R.drawable.ic_menu_send, 0xFFF59E0B,
                "Đơn chờ duyệt", () -> startActivity(new Intent(this, RequestListActivity.class))));
        allStats.add(new StatItem(R.drawable.outline_border_color_24, 0xFFEA580C,
                "Nhiệm vụ", () -> startActivity(new Intent(this, TaskActivity.class))));

        // Page 3 (index 4-7)
        allStats.add(new StatItem(R.drawable.ic_notification_bell, 0xFFEF4444,
                "TB chưa đọc", () -> startActivity(new Intent(this, NotificationCenterActivity.class))));
        allStats.add(new StatItem(R.drawable.ic_check_circle, 0xFF10B981,
                "NV đang làm", () -> startActivity(new Intent(this, EmployeeActivity.class))));
        allStats.add(new StatItem(R.drawable.ic_clock, 0xFF6B7280,
                "NV đã nghỉ", () -> startActivity(new Intent(this, EmployeeActivity.class))));

        if ("ADMIN".equals(role)) {
            allStats.add(new StatItem(android.R.drawable.ic_menu_recent_history, 0xFF8B5CF6,
                    "Nhật ký hệ thống", () -> startActivity(new Intent(this, SystemLogActivity.class))));
        } else {
            allStats.add(new StatItem(R.drawable.outline_account_circle_24, 0xFF0EA5E9,
                    "Hồ sơ cá nhân", () -> startActivity(new Intent(this, ProfileActivity.class))));
        }
    }

    // ── VIEWPAGER2 STATS (3 pages) ────────────────────────────────

    private static final int PAGE_GRID1 = 0;
    private static final int PAGE_COMPANY = 1;
    private static final int PAGE_GRID2 = 2;
    private static final int TOTAL_PAGES = 3;

    // Company settings data (cập nhật từ API)
    private String companyName = "--";
    private String companyWorkHours = "--:-- → --:--";
    private String companyRadius = "-- m";
    private String companyLocation = "Chưa thiết lập";

    private void setupStatsViewPager() {
        vpStats = findViewById(R.id.vpStats);
        layoutDots = findViewById(R.id.layoutDots);
        tvStatsPageInfo = findViewById(R.id.tvStatsPageInfo);

        vpStats.setAdapter(new StatsMultiPageAdapter());
        vpStats.getChildAt(0).setNestedScrollingEnabled(false);

        setupDots(TOTAL_PAGES);
        tvStatsPageInfo.setText("1/" + TOTAL_PAGES);
        vpStats.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int pos) {
                tvStatsPageInfo.setText((pos + 1) + "/" + TOTAL_PAGES);
                updateDots(pos);
            }
        });
    }

    private void setupDots(int count) {
        layoutDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
            p.setMargins(size / 2, 0, size / 2, 0);
            dot.setLayoutParams(p);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(i == 0 ? ContextCompat.getColor(this, R.color.colorPrimary) : 0xFFD1D5DB);
            dot.setBackground(gd);
            layoutDots.addView(dot);
        }
    }

    private void updateDots(int selected) {
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(i == selected ? ContextCompat.getColor(this, R.color.colorPrimary) : 0xFFD1D5DB);
            layoutDots.getChildAt(i).setBackground(gd);
        }
    }

    private void updateStatValue(int index, String value) {
        if (index < allStats.size()) {
            allStats.get(index).value = value;
            if (vpStats.getAdapter() != null) vpStats.getAdapter().notifyDataSetChanged();
        }
    }

    private void updateStatLabel(int index, String label) {
        if (index < allStats.size()) {
            allStats.get(index).label = label;
            if (vpStats.getAdapter() != null) vpStats.getAdapter().notifyDataSetChanged();
        }
    }

    private void updateCompanyCard() {
        if (vpStats.getAdapter() != null) vpStats.getAdapter().notifyDataSetChanged();
    }

    // ── MULTI-PAGE ADAPTER (3 view types) ─────────────────────────

    class StatsMultiPageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override public int getItemViewType(int position) { return position; }
        @Override public int getItemCount() { return TOTAL_PAGES; }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == PAGE_COMPANY) {
                return new CompanyVH(inf.inflate(R.layout.item_stat_page_company, parent, false));
            } else {
                return new GridVH(inf.inflate(R.layout.item_stat_page, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position == PAGE_COMPANY) {
                bindCompanyPage((CompanyVH) holder);
            } else {
                int startIdx = (position == PAGE_GRID1) ? 0 : 4;
                bindGridPage((GridVH) holder, startIdx);
            }
        }

        private void bindGridPage(GridVH h, int start) {
            bindCard(h.card1, h.icon1, h.value1, h.label1, start);
            bindCard(h.card2, h.icon2, h.value2, h.label2, start + 1);
            bindCard(h.card3, h.icon3, h.value3, h.label3, start + 2);
            bindCard(h.card4, h.icon4, h.value4, h.label4, start + 3);
        }

        private void bindCard(View card, ImageView icon, TextView value, TextView label, int idx) {
            if (idx < allStats.size()) {
                StatItem item = allStats.get(idx);
                card.setVisibility(View.VISIBLE);
                icon.setImageResource(item.iconRes);
                icon.setColorFilter(item.iconTint);
                value.setText(item.value);
                label.setText(item.label);
                boolean isNumber = item.value.matches("^[\\d!→✓⚠\\-]+$");
                value.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, isNumber ? 24 : 16);
                card.setOnClickListener(v -> { if (item.onClick != null) item.onClick.run(); });
            } else {
                card.setVisibility(View.INVISIBLE);
            }
        }

        private void bindCompanyPage(CompanyVH h) {
            h.tvCompanyName.setText(companyName);
            h.tvWorkHours.setText(companyWorkHours);
            h.tvRadius.setText(companyRadius);
            h.tvLocation.setText(companyLocation);
            h.cardCompany.setOnClickListener(v ->
                    startActivity(new Intent(DashboardActivity.this, CompanySettingsActivity.class)));
        }

        // ViewHolder: Grid 2x2
        class GridVH extends RecyclerView.ViewHolder {
            View card1, card2, card3, card4;
            ImageView icon1, icon2, icon3, icon4;
            TextView value1, value2, value3, value4, label1, label2, label3, label4;
            GridVH(View v) {
                super(v);
                card1 = v.findViewById(R.id.card1); card2 = v.findViewById(R.id.card2);
                card3 = v.findViewById(R.id.card3); card4 = v.findViewById(R.id.card4);
                icon1 = v.findViewById(R.id.ivIcon1); icon2 = v.findViewById(R.id.ivIcon2);
                icon3 = v.findViewById(R.id.ivIcon3); icon4 = v.findViewById(R.id.ivIcon4);
                value1 = v.findViewById(R.id.tvValue1); value2 = v.findViewById(R.id.tvValue2);
                value3 = v.findViewById(R.id.tvValue3); value4 = v.findViewById(R.id.tvValue4);
                label1 = v.findViewById(R.id.tvLabel1); label2 = v.findViewById(R.id.tvLabel2);
                label3 = v.findViewById(R.id.tvLabel3); label4 = v.findViewById(R.id.tvLabel4);
            }
        }

        // ViewHolder: Company full card
        class CompanyVH extends RecyclerView.ViewHolder {
            View cardCompany;
            TextView tvCompanyName, tvWorkHours, tvRadius, tvLocation;
            CompanyVH(View v) {
                super(v);
                cardCompany   = v.findViewById(R.id.cardCompany);
                tvCompanyName = v.findViewById(R.id.tvCompanyName);
                tvWorkHours   = v.findViewById(R.id.tvWorkHours);
                tvRadius      = v.findViewById(R.id.tvRadius);
                tvLocation    = v.findViewById(R.id.tvLocation);
            }
        }
    }

    // ── QUICK ACCESS NAVIGATION ───────────────────────────────────

    private void setupNavigation() {
        safeClick(R.id.btnNavEmployee,     () -> startActivity(new Intent(this, EmployeeActivity.class)));
        safeClick(R.id.btnNavDepartment,   () -> startActivity(new Intent(this, DepartmentActivity.class)));
        safeClick(R.id.btnNavNotification, () -> startActivity(new Intent(this, NotificationCenterActivity.class)));
        safeClick(R.id.btnNavRequest,      () -> startActivity(new Intent(this, RequestListActivity.class)));
        safeClick(R.id.btnNavAccount, () -> {
            if ("ADMIN".equals(role)) startActivity(new Intent(this, AccountManagementActivity.class));
            else startActivity(new Intent(this, ProfileActivity.class));
        });
        safeClick(R.id.btnNavLogout, this::showLogoutDialog);
    }

    // ── BOTTOM NAVIGATION ─────────────────────────────────────────

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Admin: hiện tất cả tabs. Employee/Manager: ẩn Cài đặt + Nhật ký
        if (!"ADMIN".equals(role)) {
            bottomNav.getMenu().findItem(R.id.nav_settings).setVisible(false);
            bottomNav.getMenu().findItem(R.id.nav_logs).setVisible(false);
        }

        // Default: Trang chủ selected
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Đã ở trang chủ, scroll lên đầu
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, CompanySettingsActivity.class));
                return true;
            } else if (id == R.id.nav_logs) {
                startActivity(new Intent(this, SystemLogActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    // ── LOAD STATS ────────────────────────────────────────────────

    private void loadStats() {
        // 0: Tổng NV + 5: NV đang làm + 6: NV đã nghỉ
        apiService.getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override public void onResponse(Call<List<Employee>> c, Response<List<Employee>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    List<Employee> list = r.body();
                    updateStatValue(0, String.valueOf(list.size()));
                    long active = list.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatusRaw())).count();
                    updateStatValue(5, String.valueOf(active));
                    updateStatValue(6, String.valueOf(list.size() - active));
                } else if (r.code() == 401) { handleSessionExpired(); }
            }
            @Override public void onFailure(Call<List<Employee>> c, Throwable t) { updateStatValue(0, "!"); }
        });

        // 1: Phòng ban
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override public void onResponse(Call<List<Department>> c, Response<List<Department>> r) {
                if (r.isSuccessful() && r.body() != null) updateStatValue(1, String.valueOf(r.body().size()));
            }
            @Override public void onFailure(Call<List<Department>> c, Throwable t) { updateStatValue(1, "!"); }
        });

        // 2: Đơn chờ duyệt
        Call<List<RequestModels.RequestResponse>> reqCall =
                "ADMIN".equals(role) ? apiService.getAllRequests() : apiService.getMyRequests(employeeId);
        reqCall.enqueue(new Callback<List<RequestModels.RequestResponse>>() {
            @Override public void onResponse(Call<List<RequestModels.RequestResponse>> c,
                                             Response<List<RequestModels.RequestResponse>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    long pending = r.body().stream().filter(req -> "PENDING".equals(req.status)).count();
                    updateStatValue(2, String.valueOf(pending));
                }
            }
            @Override public void onFailure(Call<List<RequestModels.RequestResponse>> c, Throwable t) { updateStatValue(2, "!"); }
        });

        // 3: Nhiệm vụ
        apiService.getMyTasks(employeeId).enqueue(new Callback<List<TaskModels.TaskResponse>>() {
            @Override public void onResponse(Call<List<TaskModels.TaskResponse>> c,
                                             Response<List<TaskModels.TaskResponse>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    long notDone = r.body().stream().filter(t -> !"DONE".equals(t.status)).count();
                    updateStatValue(3, String.valueOf(notDone));
                }
            }
            @Override public void onFailure(Call<List<TaskModels.TaskResponse>> c, Throwable t) { updateStatValue(3, "!"); }
        });

        // 4: TB chưa đọc
        apiService.getUnreadCount(userId).enqueue(new Callback<Long>() {
            @Override public void onResponse(Call<Long> c, Response<Long> r) {
                if (r.isSuccessful() && r.body() != null) updateStatValue(4, String.valueOf(r.body()));
            }
            @Override public void onFailure(Call<Long> c, Throwable t) { updateStatValue(4, "!"); }
        });

        // Company settings → trang 2 full card
        apiService.getCompanySettings().enqueue(new Callback<AdminModels.CompanySettings>() {
            @Override public void onResponse(Call<AdminModels.CompanySettings> c,
                                             Response<AdminModels.CompanySettings> r) {
                if (r.isSuccessful() && r.body() != null) {
                    AdminModels.CompanySettings s = r.body();
                    companyName = s.companyName != null ? s.companyName : "Chưa đặt tên";
                    if (s.workStartTime != null && s.workEndTime != null) {
                        companyWorkHours = s.workStartTime + " → " + s.workEndTime;
                    }
                    if (s.allowedRadius != null) {
                        companyRadius = s.allowedRadius + " m";
                    }
                    if (s.baseLat != null && s.baseLng != null) {
                        companyLocation = String.format(java.util.Locale.US,
                                "%.5f, %.5f", s.baseLat, s.baseLng);
                    }
                } else {
                    companyName = "Chưa cấu hình";
                }
                updateCompanyCard();
            }
            @Override public void onFailure(Call<AdminModels.CompanySettings> c, Throwable t) {
                companyName = "Lỗi kết nối";
                updateCompanyCard();
            }
        });
    }

    // ── SESSION / LOGOUT ──────────────────────────────────────────

    private void handleSessionExpired() {
        Toast.makeText(this, "Phiên đăng nhập hết hạn", Toast.LENGTH_LONG).show();
        String saved = prefs.getString("saved_username", null);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        if (saved != null) editor.putString("saved_username", saved);
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

    // ── UTILS ─────────────────────────────────────────────────────

    private void safeClick(int id, Runnable action) {
        View v = findViewById(id);
        if (v != null) {
            v.setOnClickListener(view -> action.run());
            v.setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            });
        }
    }

    @Override protected void onResume() {
        super.onResume();
        loadStats();
        // Reset bottom nav về Home khi quay lại
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_home);
    }
    @Override protected void onDestroy() { super.onDestroy(); timeHandler.removeCallbacksAndMessages(null); }
}