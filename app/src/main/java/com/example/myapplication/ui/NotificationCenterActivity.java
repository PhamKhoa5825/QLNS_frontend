package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
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
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationCenterActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvUnreadCount;
    private ApiService apiService;
    private SharedPreferences prefs;
    private Long userId;
    private Long employeeId;
    private String role;
    private NotificationAdapter adapter;
    private List<NotificationModels.NotificationResponse> allNotis = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_notification_center);

        // Đẩy btnBack xuống bằng chiều cao status bar
        View btnBack = findViewById(R.id.btnBack);
        ViewCompat.setOnApplyWindowInsetsListener(btnBack, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int dp48 = (int) (48 * getResources().getDisplayMetrics().density);
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) v.getLayoutParams();
            params.topMargin = dp48 + statusBarHeight;
            v.setLayoutParams(params);
            return insets;
        });

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        userId     = prefs.getLong("userId", -1);
        employeeId = prefs.getLong("employeeId", -1);
        role       = prefs.getString("role", "EMPLOYEE");
        apiService = RetrofitClient.getClient().create(ApiService.class);

        recyclerView  = findViewById(R.id.recyclerViewNotifications);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        if (findViewById(R.id.btnMarkAllRead) != null) {
            findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> markAllRead());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>(), this::onMarkRead);
        recyclerView.setAdapter(adapter);

        // Lấy departmentId từ employee info trước, rồi mới load notifications
        fetchDeptAndLoadNotifications();
    }

    private void fetchDeptAndLoadNotifications() {
        Long savedDeptId = prefs.getLong("departmentId", -1);
        if (savedDeptId != -1) {
            loadNotifications(savedDeptId);
            return;
        }

        // departmentId chưa có → gọi API lấy employee info
        apiService.getEmployeeById(employeeId).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Long deptId = response.body().getDepartmentId();
                    if (deptId != null) {
                        prefs.edit().putLong("departmentId", deptId).apply();
                        loadNotifications(deptId);
                    } else {
                        Toast.makeText(NotificationCenterActivity.this,
                                "Không tìm thấy phòng ban", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(NotificationCenterActivity.this,
                            "Lỗi lấy thông tin nhân viên: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                Toast.makeText(NotificationCenterActivity.this,
                        "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotifications(Long deptId) {
        apiService.getNotifications(deptId, userId)
                .enqueue(new Callback<List<NotificationModels.NotificationResponse>>() {
                    @Override
                    public void onResponse(Call<List<NotificationModels.NotificationResponse>> call,
                                           Response<List<NotificationModels.NotificationResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allNotis = response.body();
                            adapter.updateData(allNotis);
                            updateUnreadCountDisplay();
                        }
                    }
                    @Override public void onFailure(Call<List<NotificationModels.NotificationResponse>> call, Throwable t) {
                        Toast.makeText(NotificationCenterActivity.this, "Lỗi tải thông báo", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUnreadCountDisplay() {
        if (tvUnreadCount != null) {
            long unread = allNotis.stream().filter(n -> !n.isRead).count();
            tvUnreadCount.setText(unread + " thông báo chưa đọc");
        }
    }

    private void onMarkRead(NotificationModels.NotificationResponse noti) {
        if (noti.isRead) return;
        apiService.markNotiRead(noti.id, userId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            noti.isRead = true;
                            adapter.notifyDataSetChanged();
                            updateUnreadCountDisplay();
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
    }

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
        updateUnreadCountDisplay();
        Toast.makeText(this, "Đã đánh dấu tất cả đã đọc", Toast.LENGTH_SHORT).show();
    }
}