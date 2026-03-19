package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapter.NotificationAdapter;
import com.example.myapplication.model.Employee;
import com.example.myapplication.model.NotificationModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationCenterActivity extends AppCompatActivity
        implements NotificationAdapter.OnActionListener {

    private RecyclerView recyclerView;
    private TextView tvUnreadCount;
    private FloatingActionButton fabCreate;
    private ApiService apiService;
    private SharedPreferences prefs;
    private Long userId, employeeId;
    private String role;
    private boolean isAdmin;
    private NotificationAdapter adapter;
    private List<NotificationModels.NotificationResponse> allNotis = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_notification_center);

        View btnBack = findViewById(R.id.btnBack);
        ViewCompat.setOnApplyWindowInsetsListener(btnBack, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int dp48 = (int) (48 * getResources().getDisplayMetrics().density);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            params.topMargin = dp48 + statusBarHeight;
            v.setLayoutParams(params);
            return insets;
        });

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        userId     = prefs.getLong("userId", -1);
        employeeId = prefs.getLong("employeeId", -1);
        role       = prefs.getString("role", "EMPLOYEE");
        isAdmin    = "ADMIN".equals(role);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        recyclerView  = findViewById(R.id.recyclerViewNotifications);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);

        btnBack.setOnClickListener(v -> finish());
        if (findViewById(R.id.btnMarkAllRead) != null)
            findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> markAllRead());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>(), this, isAdmin);
        recyclerView.setAdapter(adapter);

        // FAB tạo thông báo — chỉ Admin
        fabCreate = findViewById(R.id.fabCreateNoti);
        if (fabCreate != null) {
            if (isAdmin) {
                fabCreate.setVisibility(View.VISIBLE);
                fabCreate.setOnClickListener(v ->
                        startActivity(new Intent(this, CreateNotificationActivity.class)));
            } else {
                fabCreate.setVisibility(View.GONE);
            }
        }

        fetchDeptAndLoadNotifications();
    }

    // ── LOAD DATA ─────────────────────────────────────────────────

    private void fetchDeptAndLoadNotifications() {
        Long savedDeptId = prefs.getLong("departmentId", -1);
        if (savedDeptId != -1) { loadNotifications(savedDeptId); return; }

        if (employeeId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override public void onResponse(Call<Employee> c, Response<Employee> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Long deptId = r.body().getDepartmentId();
                    if (deptId != null) {
                        prefs.edit().putLong("departmentId", deptId).apply();
                        loadNotifications(deptId);
                    } else {
                        loadNotifications(0L);
                    }
                } else {
                    Toast.makeText(NotificationCenterActivity.this,
                            "Lỗi " + r.code() + " (empId=" + employeeId + ")", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Employee> c, Throwable t) {
                Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotifications(Long deptId) {
        apiService.getNotifications(deptId, userId)
                .enqueue(new Callback<List<NotificationModels.NotificationResponse>>() {
                    @Override public void onResponse(Call<List<NotificationModels.NotificationResponse>> c,
                                                     Response<List<NotificationModels.NotificationResponse>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            allNotis = r.body();
                            adapter.updateData(allNotis);
                            updateUnreadCount();
                        }
                    }
                    @Override public void onFailure(Call<List<NotificationModels.NotificationResponse>> c, Throwable t) {
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
        if (noti.isRead) return;
        apiService.markNotiRead(noti.id, userId).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    noti.isRead = true;
                    adapter.notifyDataSetChanged();
                    updateUnreadCount();
                }
            }
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
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
                        @Override public void onResponse(Call<Void> c, Response<Void> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(NotificationCenterActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show();
                                allNotis.remove(noti);
                                adapter.updateData(allNotis);
                                updateUnreadCount();
                            } else {
                                Toast.makeText(NotificationCenterActivity.this, "Lỗi: " + r.code(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<Void> c, Throwable t) {
                            Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Huỷ", null).show();
    }

    // ── EDIT DIALOG ───────────────────────────────────────────────

    private void showEditDialog(NotificationModels.NotificationResponse noti) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_notification, null);
        TextInputEditText etTitle   = view.findViewById(R.id.etEditTitle);
        TextInputEditText etContent = view.findViewById(R.id.etEditContent);

        etTitle.setText(noti.title);
        etContent.setText(noti.content);

        new AlertDialog.Builder(this)
                .setTitle("Sửa thông báo")
                .setView(view)
                .setPositiveButton("Lưu", (d, w) -> {
                    String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                    String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
                    if (title.isEmpty()) { Toast.makeText(this, "Tiêu đề không được trống", Toast.LENGTH_SHORT).show(); return; }

                    apiService.updateNotification(noti.id, new NotificationModels.UpdateNotificationRequest(title, content))
                            .enqueue(new Callback<NotificationModels.NotificationResponse>() {
                                @Override public void onResponse(Call<NotificationModels.NotificationResponse> c,
                                                                 Response<NotificationModels.NotificationResponse> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(NotificationCenterActivity.this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                                        noti.title = title;
                                        noti.content = content;
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        Toast.makeText(NotificationCenterActivity.this, "Lỗi: " + r.code(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override public void onFailure(Call<NotificationModels.NotificationResponse> c, Throwable t) {
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

    @Override protected void onResume() {
        super.onResume();
        fetchDeptAndLoadNotifications();
    }
}