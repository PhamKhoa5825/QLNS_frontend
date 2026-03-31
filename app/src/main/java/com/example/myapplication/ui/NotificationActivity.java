package com.example.myapplication.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.NotificationAdapter;
import com.example.myapplication.model.CreateNotificationRequest;
import com.example.myapplication.model.Notification;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView recyclerViewNoti;
    private NotificationAdapter adapter;
    private List<Notification> allNotiList = new ArrayList<>();
    private List<Notification> currentList = new ArrayList<>();
    
    private ImageView btnBack;
    private TextView tvHeaderUnreadCount;
    private TextView tabAll, tabUnread, tabRead;
    private ImageView btnMarkAllRead;
    
    private Long currentDeptId;
    private Long currentUserId;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();
        currentUserId = SharedPrefsManager.getInstance(this).getUserId();

        initViews();
        setupRecyclerView();
        setupFilters();

        btnBack.setOnClickListener(v -> finish());
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        fetchNotifications();
    }

    private void initViews() {
        recyclerViewNoti = findViewById(R.id.recyclerViewNoti);
        btnBack = findViewById(R.id.btnBackNoti);
        tvHeaderUnreadCount = findViewById(R.id.tvHeaderUnreadCount);
        tabAll = findViewById(R.id.tabAll);
        tabUnread = findViewById(R.id.tabUnread);
        tabRead = findViewById(R.id.tabRead);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
    }

    private void setupRecyclerView() {
        recyclerViewNoti.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, currentList, this);
        recyclerViewNoti.setAdapter(adapter);
    }

    private void setupFilters() {
        tabAll.setOnClickListener(v -> applyFilter("ALL"));
        tabUnread.setOnClickListener(v -> applyFilter("UNREAD"));
        tabRead.setOnClickListener(v -> applyFilter("READ"));
    }

    private void fetchNotifications() {
        ApiService apiService = RetrofitClient.getApiService(this);
        String role = SharedPrefsManager.getInstance(this).getRole();
        
        if ("ADMIN".equals(role)) {
            // Admin xem tất cả notifications
            apiService.getAllNotifications().enqueue(new Callback<List<Notification>>() {
                @Override
                public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        allNotiList = response.body();
                        updateStats();
                        applyFilter(currentFilter);
                    } else {
                        Toast.makeText(NotificationActivity.this, "Không thể tải thông báo: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<List<Notification>> call, Throwable t) {
                    Toast.makeText(NotificationActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Manager/Employee xem thông báo theo phòng ban
            apiService.getNotifications(currentDeptId, currentUserId).enqueue(new Callback<List<Notification>>() {
                @Override
                public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        allNotiList = response.body();
                        updateStats();
                        applyFilter(currentFilter);
                    } else {
                        Toast.makeText(NotificationActivity.this, "Không thể tải thông báo: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<List<Notification>> call, Throwable t) {
                    Toast.makeText(NotificationActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateStats() {
        int unread = 0;
        for (Notification n : allNotiList) {
            if (!n.isRead()) unread++;
        }
        tvHeaderUnreadCount.setText(String.valueOf(unread));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        
        // Update Tabs UI
        tabAll.setBackgroundResource(filter.equals("ALL") ? R.drawable.bg_tab_selected : 0);
        tabAll.setTextColor(filter.equals("ALL") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#4B5563"));
        
        tabUnread.setBackgroundResource(filter.equals("UNREAD") ? R.drawable.bg_tab_selected : 0);
        tabUnread.setTextColor(filter.equals("UNREAD") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#4B5563"));
        
        tabRead.setBackgroundResource(filter.equals("READ") ? R.drawable.bg_tab_selected : 0);
        tabRead.setTextColor(filter.equals("READ") ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#4B5563"));

        List<Notification> filtered = new ArrayList<>();
        if (filter.equals("ALL")) {
            filtered.addAll(allNotiList);
        } else if (filter.equals("UNREAD")) {
            for (Notification n : allNotiList) if (!n.isRead()) filtered.add(n);
        } else {
            for (Notification n : allNotiList) if (n.isRead()) filtered.add(n);
        }
        
        currentList.clear();
        currentList.addAll(filtered);
        adapter.setNotiList(currentList);
    }

    @Override
    public void onNotificationClick(Notification noti) {
        if (!noti.isRead()) {
            markAsRead(noti);
        }
        // Could show detail dialog here
    }

    private void markAsRead(Notification noti) {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.markNotificationAsRead(noti.getId(), currentUserId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    noti.setRead(true);
                    updateStats();
                    applyFilter(currentFilter);
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void markAllAsRead() {
        for (Notification n : allNotiList) {
            if (!n.isRead()) markAsRead(n); // Sequential for now, or could have a bulk API
        }
    }
}
