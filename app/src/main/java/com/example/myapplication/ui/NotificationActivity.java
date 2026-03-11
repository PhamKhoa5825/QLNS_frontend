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
    private FloatingActionButton fabAddNoti;
    private TextView tvHeaderUnreadCount, tvCountUnread, tvCountTotal;
    private TextView tabAll, tabUnread, tabRead;
    private LinearLayout btnMarkAllRead;
    
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
        fabAddNoti.setOnClickListener(v -> showCreateNotiDialog());
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        fetchNotifications();
    }

    private void initViews() {
        recyclerViewNoti = findViewById(R.id.recyclerViewNoti);
        btnBack = findViewById(R.id.btnBackNoti);
        fabAddNoti = findViewById(R.id.fabAddNotification);
        tvHeaderUnreadCount = findViewById(R.id.tvHeaderUnreadCount);
        tvCountUnread = findViewById(R.id.tvCountUnread);
        tvCountTotal = findViewById(R.id.tvCountTotal);
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

    private void updateStats() {
        int total = allNotiList.size();
        int unread = 0;
        for (Notification n : allNotiList) {
            if (!n.isRead()) unread++;
        }
        tvCountTotal.setText(String.valueOf(total));
        tvCountUnread.setText(String.valueOf(unread));
        tvHeaderUnreadCount.setText(String.valueOf(unread));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        
        // Update Tabs UI
        tabAll.setBackgroundResource(filter.equals("ALL") ? R.drawable.bg_tab_selected : 0);
        tabUnread.setBackgroundResource(filter.equals("UNREAD") ? R.drawable.bg_tab_selected : 0);
        tabRead.setBackgroundResource(filter.equals("READ") ? R.drawable.bg_tab_selected : 0);

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

    private void showCreateNotiDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_notification_form);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        EditText edtTitle = dialog.findViewById(R.id.edtNotiTitle);
        EditText edtMessage = dialog.findViewById(R.id.edtNotiMessage);
        Spinner spinnerType = dialog.findViewById(R.id.spinnerNotiType);
        Button btnSend = dialog.findViewById(R.id.btnSendNotification);

        String[] types = {"DEPARTMENT", "COMPANY"};
        ArrayAdapter<String> adapterType = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapterType);
        
        btnSend.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String msg = edtMessage.getText().toString().trim();
            String targetType = types[spinnerType.getSelectedItemPosition()];
            
            if (title.isEmpty() || msg.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            
            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setTitle(title);
            req.setContent(msg);
            req.setTargetType(targetType);
            req.setDepartmentId("DEPARTMENT".equals(targetType) ? currentDeptId : null);
            req.setCreatedById(currentUserId);
            
            sendNotification(req, dialog);
        });
        
        dialog.show();
    }
    
    private void sendNotification(CreateNotificationRequest noti, Dialog dialog) {
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<Notification> call = apiService.createNotification(noti);
        call.enqueue(new Callback<Notification>() {
            @Override
            public void onResponse(Call<Notification> call, Response<Notification> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(NotificationActivity.this, "Đã gửi thông báo", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    fetchNotifications();
                } else {
                    Toast.makeText(NotificationActivity.this, "Lỗi gửi", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Notification> call, Throwable t) {
                Toast.makeText(NotificationActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
