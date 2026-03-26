package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.NotificationAdapter;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.NotificationModels;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * NotificationCenterActivity v2 — Sửa 3 vấn đề:
 * 1. Header chuẩn (AppBarLayout + MaterialToolbar) giống các trang khác
 * 2. Tabs: "Tất cả" | "Đã gửi" (Admin thấy TB mình đã tạo)
 * 3. Click vào TB → mở NotificationDetailActivity
 */
public class NotificationCenterActivity extends AppCompatActivity
        implements NotificationAdapter.OnActionListener {

    private RecyclerView recyclerView;
    private TextView tvUnreadCount, tvEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabCreate;
    private TabLayout tabLayout;
    private ApiService apiService;
    private SharedPreferences prefs;
    private Long userId, employeeId;
    private String role;
    private boolean isAdmin;
    private NotificationAdapter adapter;
    private List<NotificationModels.NotificationResponse> allNotis = new ArrayList<>();
    private List<NotificationModels.NotificationResponse> sentNotis = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_notification_center);

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        userId     = prefs.getLong("userId", -1);
        employeeId = prefs.getLong("employeeId", -1);
        role       = prefs.getString("role", "EMPLOYEE");
        isAdmin    = "ADMIN".equals(role) || "MANAGER".equals(role);

        bindViews();
        setupTabs();
        fetchDeptAndLoadNotifications();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView  = findViewById(R.id.recyclerViewNotifications);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);
        tvEmpty       = findViewById(R.id.tvEmpty);
        progressBar   = findViewById(R.id.progressBar);
        fabCreate     = findViewById(R.id.fabCreateNoti);
        tabLayout     = findViewById(R.id.tabLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>(), this, isAdmin);
        recyclerView.setAdapter(adapter);

        // Nút đọc tất cả
        View btnMarkAll = findViewById(R.id.btnMarkAllRead);
        if (btnMarkAll != null) {
            btnMarkAll.setOnClickListener(v -> markAllRead());
        }

        // FAB tạo TB (Admin/Manager)
        if (isAdmin) {
            fabCreate.setVisibility(View.VISIBLE);
            fabCreate.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateNotificationActivity.class)));
        } else {
            fabCreate.setVisibility(View.GONE);
        }
    }

    // ── TABS ──────────────────────────────────────────────────────

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));

        // Chỉ Admin/Manager mới thấy tab "Đã gửi"
        if (isAdmin) {
            tabLayout.addTab(tabLayout.newTab().setText("Đã gửi"));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showTab(int position) {
        if (position == 0) {
            // Tab "Tất cả" — TB nhận được
            adapter.updateData(allNotis);
            tvEmpty.setVisibility(allNotis.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText("Không có thông báo");
            tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            // Tab "Đã gửi" — TB mình đã tạo
            filterSentNotifications();
            adapter.updateData(sentNotis);
            tvEmpty.setVisibility(sentNotis.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText("Bạn chưa gửi thông báo nào");
            tvUnreadCount.setVisibility(View.GONE);
        }
    }

    /**
     * Lọc TB do mình tạo — dựa vào createdByName match với tên NV hiện tại.
     * Cách đơn giản nhất mà không cần API mới.
     */
    private void filterSentNotifications() {
        String myName = prefs.getString("fullName", "");
        sentNotis = allNotis.stream()
                .filter(n -> n.createdByName != null && n.createdByName.equals(myName))
                .collect(Collectors.toList());
    }

    // ── LOAD DATA ─────────────────────────────────────────────────

    private void fetchDeptAndLoadNotifications() {
        Long savedDeptId = prefs.getLong("departmentId", -1);
        if (savedDeptId != -1) {
            loadNotifications(savedDeptId);
            return;
        }

        if (employeeId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> c, Response<Employee> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Long deptId = r.body().getDepartmentId();
                    if (deptId != null) {
                        prefs.edit().putLong("departmentId", deptId).apply();
                        loadNotifications(deptId);
                    } else {
                        loadNotifications(0L);
                    }
                }
            }
            @Override
            public void onFailure(Call<Employee> c, Throwable t) {
                Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotifications(Long deptId) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getNotifications(deptId, userId)
                .enqueue(new Callback<List<NotificationModels.NotificationResponse>>() {
                    @Override
                    public void onResponse(Call<List<NotificationModels.NotificationResponse>> c,
                                           Response<List<NotificationModels.NotificationResponse>> r) {
                        progressBar.setVisibility(View.GONE);
                        if (r.isSuccessful() && r.body() != null) {
                            allNotis = r.body();

                            // Hiện theo tab đang chọn
                            int selectedTab = tabLayout.getSelectedTabPosition();
                            showTab(selectedTab);
                            updateUnreadCount();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<NotificationModels.NotificationResponse>> c, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NotificationCenterActivity.this, "Lỗi tải thông báo", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUnreadCount() {
        if (tvUnreadCount != null) {
            long unread = allNotis.stream().filter(n -> !n.isRead).count();
            tvUnreadCount.setText(unread + " thông báo chưa đọc");
        }
    }

    // ── ADAPTER CALLBACKS ─────────────────────────────────────────

    @Override
    public void onMarkRead(NotificationModels.NotificationResponse noti) {
        // MỚI: Click vào TB → mở trang chi tiết (thay vì chỉ mark read)
        Intent intent = new Intent(this, NotificationDetailActivity.class);
        intent.putExtra("notiId", noti.id);
        intent.putExtra("title", noti.title);
        intent.putExtra("content", noti.content);
        intent.putExtra("targetType", noti.targetType);
        intent.putExtra("departmentName", noti.departmentName);
        intent.putExtra("createdByName", noti.createdByName);
        intent.putExtra("createdAt", noti.createdAt);
        intent.putExtra("userId", userId);
        startActivity(intent);

        // Đánh dấu đã đọc local ngay (không đợi API)
        if (!noti.isRead) {
            noti.isRead = true;
            adapter.notifyDataSetChanged();
            updateUnreadCount();

            apiService.markNotiRead(noti.id, userId).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                @Override public void onFailure(Call<Void> c, Throwable t) {}
            });
        }
    }

    @Override
    public void onEdit(NotificationModels.NotificationResponse noti) {
        showEditDialog(noti);
    }

    @Override
    public void onDelete(NotificationModels.NotificationResponse noti) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thông báo")
                .setMessage("Xóa \"" + noti.title + "\"?")
                .setPositiveButton("Xóa", (d, w) -> {
                    apiService.deleteNotification(noti.id).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> c, Response<Void> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(NotificationCenterActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show();
                                allNotis.remove(noti);
                                int selectedTab = tabLayout.getSelectedTabPosition();
                                showTab(selectedTab);
                                updateUnreadCount();
                            } else {
                                ApiErrorHelper.show(NotificationCenterActivity.this, r, "Xóa thông báo thất bại");
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> c, Throwable t) {
                            Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Huỷ", null).show();
    }

    // ── EDIT DIALOG ───────────────────────────────────────────────

    private void showEditDialog(NotificationModels.NotificationResponse noti) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_notification, null);
        TextInputEditText etTitle = view.findViewById(R.id.etEditTitle);
        TextInputEditText etContent = view.findViewById(R.id.etEditContent);

        etTitle.setText(noti.title);
        etContent.setText(noti.content);

        new AlertDialog.Builder(this)
                .setTitle("Sửa thông báo")
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Tiêu đề không được trống", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    apiService.updateNotification(noti.id, new NotificationModels.UpdateNotificationRequest(title, content))
                            .enqueue(new Callback<NotificationModels.NotificationResponse>() {
                                @Override
                                public void onResponse(Call<NotificationModels.NotificationResponse> c,
                                                       Response<NotificationModels.NotificationResponse> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(NotificationCenterActivity.this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                                        noti.title = title;
                                        noti.content = content;
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        ApiErrorHelper.show(NotificationCenterActivity.this, r, "Cập nhật thông báo thất bại");
                                    }
                                }
                                @Override
                                public void onFailure(Call<NotificationModels.NotificationResponse> c, Throwable t) {
                                    Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null).show();
    }

    // ── MARK ALL READ ─────────────────────────────────────────────

    private void markAllRead() {
        for (NotificationModels.NotificationResponse noti : allNotis) {
            if (!noti.isRead) {
                apiService.markNotiRead(noti.id, userId).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                    @Override public void onFailure(Call<Void> c, Throwable t) {}
                });
                noti.isRead = true;
            }
        }
        adapter.notifyDataSetChanged();
        updateUnreadCount();
        Toast.makeText(this, "Đã đánh dấu tất cả đã đọc", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDeptAndLoadNotifications();
    }
}
