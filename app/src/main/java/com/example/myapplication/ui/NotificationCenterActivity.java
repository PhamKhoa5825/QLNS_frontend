package com.example.myapplication.ui;

import android.content.Intent;
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
import com.example.myapplication.model.dto.NotificationDto;
import com.example.myapplication.model.entity.Employee;
import com.example.myapplication.model.entity.Notification;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
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

public class NotificationCenterActivity extends AppCompatActivity
        implements NotificationAdapter.OnActionListener {

    private RecyclerView recyclerView;
    private TextView tvUnreadCount, tvEmpty;
    private ProgressBar progressBar;
    private FloatingActionButton fabCreate;
    private TabLayout tabLayout;

    private ApiService apiService;
    private SharedPrefsManager pm;
    private Long userId, employeeId;
    private boolean isAdmin;
    private NotificationAdapter adapter;
    private List<Notification> allNotis = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_notification_center);

        pm = SharedPrefsManager.getInstance(this);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        userId = pm.getUserId();
        employeeId = pm.getEmployeeId();
        isAdmin = pm.isAdminOrManager();

        bindViews();
        setupTabs();
        fetchDeptAndLoad();
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

        View btnMarkAll = findViewById(R.id.btnMarkAllRead);
        if (btnMarkAll != null) btnMarkAll.setOnClickListener(v -> markAllRead());

        if (isAdmin) {
            fabCreate.setVisibility(View.VISIBLE);
            fabCreate.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateNotificationActivity.class)));
        } else {
            fabCreate.setVisibility(View.GONE);
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        if (isAdmin) tabLayout.addTab(tabLayout.newTab().setText("Đã gửi"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { showTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── TABS ──────────────────────────────────────────────────

    private void showTab(int position) {
        if (position == 0) {
            adapter.updateData(allNotis);
            tvEmpty.setVisibility(allNotis.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText("Không có thông báo");
            tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            // Tab "Đã gửi" — lọc TB mình tạo
            String myName = pm.getFullName();
            List<Notification> sent = allNotis.stream()
                    .filter(n -> n.createdByName != null && n.createdByName.equals(myName))
                    .collect(Collectors.toList());
            adapter.updateData(sent);
            tvEmpty.setVisibility(sent.isEmpty() ? View.VISIBLE : View.GONE);
            tvEmpty.setText("Bạn chưa gửi thông báo nào");
            tvUnreadCount.setVisibility(View.GONE);
        }
    }

    // ── LOAD ──────────────────────────────────────────────────

    private void fetchDeptAndLoad() {
        Long deptId = pm.getDepartmentId();
        if (deptId != -1) {
            loadNotifications(deptId);
            return;
        }

        if (employeeId == -1) return;

        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override public void onResponse(Call<Employee> c, Response<Employee> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Long dId = r.body().getDepartmentId();
                    if (dId != null) {
                        pm.saveDepartmentId(dId);
                        loadNotifications(dId);
                    } else {
                        loadNotifications(0L);
                    }
                } else {
                    ApiErrorHelper.show(NotificationCenterActivity.this, r, "Lỗi tải thông tin nhân viên");
                }
            }
            @Override public void onFailure(Call<Employee> c, Throwable t) {
                Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotifications(Long deptId) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getNotifications(deptId, userId).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> c, Response<List<Notification>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    allNotis = r.body();
                    showTab(tabLayout.getSelectedTabPosition());
                    updateUnreadCount();
                } else {
                    Toast.makeText(NotificationCenterActivity.this,
                            ApiErrorHelper.parse(r, "Lỗi tải thông báo"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUnreadCount() {
        long unread = allNotis.stream().filter(n -> !n.isRead).count();
        tvUnreadCount.setText(unread + " thông báo chưa đọc");
    }

    // ── ADAPTER CALLBACKS ─────────────────────────────────────

    @Override
    public void onMarkRead(Notification noti) {
        // Click → mở detail
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

        if (!noti.isRead) {
            noti.isRead = true;
            adapter.notifyDataSetChanged();
            updateUnreadCount();
            apiService.markNotiRead(noti.id, userId).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> c, Response<Void> r) { /* silent */ }
                @Override public void onFailure(Call<Void> c, Throwable t) { /* silent */ }
            });
        }
    }

    @Override
    public void onEdit(Notification noti) {
        showEditDialog(noti);
    }

    @Override
    public void onDelete(Notification noti) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thông báo")
                .setMessage("Xóa \"" + noti.title + "\"?")
                .setPositiveButton("Xóa", (d, w) -> deleteNotification(noti.id))
                .setNegativeButton("Huỷ", null).show();
    }

    private void deleteNotification(Long notiId) {
        apiService.deleteNotification(notiId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    Toast.makeText(NotificationCenterActivity.this, "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                    fetchDeptAndLoad(); // Reload
                } else {
                    Toast.makeText(NotificationCenterActivity.this,
                            ApiErrorHelper.parse(r, "Xóa thông báo thất bại"),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── EDIT DIALOG (giữ ApiService + ApiErrorHelper) ─────────

    private void showEditDialog(Notification noti) {
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

                    apiService.updateNotification(noti.id,
                                    new NotificationDto.UpdateNotificationRequest(title, content))
                            .enqueue(new Callback<Notification>() {
                                @Override
                                public void onResponse(Call<Notification> c,
                                                       Response<Notification> r) {
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
                                public void onFailure(Call<Notification> c, Throwable t) {
                                    Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null).show();
    }

    // ── MARK ALL READ ─────────────────────────────────────────

    private void markAllRead() {
        for (Notification noti : allNotis) {
            if (!noti.isRead) {
                noti.isRead = true;
                apiService.markNotiRead(noti.id, userId).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c, Response<Void> r) { /* silent */ }
                    @Override public void onFailure(Call<Void> c, Throwable t) { /* silent */ }
                });
            }
        }
        adapter.notifyDataSetChanged();
        updateUnreadCount();
        Toast.makeText(this, "Đã đánh dấu tất cả đã đọc", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchDeptAndLoad();
    }
}
