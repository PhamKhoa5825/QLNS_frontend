package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.NotificationAdapter;
import com.example.myapplication.model.Notification;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationCenterActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private TextView tvUnreadCount;
    private LinearLayout btnMarkAllRead;
    
    private ApiService apiService;
    private Long currentUserId;
    private Long currentDeptId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        apiService = RetrofitClient.getApiService();
        currentUserId = SharedPrefsManager.getInstance(this).getUserId();
        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();

        initViews();
        setupRecyclerView();
        fetchNotifications();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewNotifications);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, notificationList, this::handleNotificationClick);
        recyclerView.setAdapter(adapter);
    }

    private void fetchNotifications() {
        if (currentDeptId == null) return;

        apiService.getNotifications(currentDeptId, currentUserId).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    notificationList = response.body();
                    adapter.setNotificationList(notificationList);
                    updateUnreadCount();
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {
                Toast.makeText(NotificationCenterActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUnreadCount() {
        int count = 0;
        for (Notification n : notificationList) {
            if (!n.isRead()) count++;
        }
        if (count > 0) {
            tvUnreadCount.setText(count + " thông báo chưa đọc");
            tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            tvUnreadCount.setText("Không có thông báo mới");
        }
    }

    private void handleNotificationClick(Notification notification) {
        if (!notification.isRead()) {
            apiService.markNotificationAsRead(notification.getId(), currentUserId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        notification.setRead(true);
                        adapter.notifyDataSetChanged();
                        updateUnreadCount();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        }
    }

    private void markAllAsRead() {
        for (Notification n : notificationList) {
            if (!n.isRead()) {
                handleNotificationClick(n);
            }
        }
    }
}
