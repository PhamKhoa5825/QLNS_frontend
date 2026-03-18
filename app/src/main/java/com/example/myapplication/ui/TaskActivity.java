package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.TaskModels;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private TaskAdapter adapter;
    private ApiService apiService;
    private SharedPreferences prefs;
    private Long employeeId;
    private String role;
    private MaterialToolbar topAppBar;

    private List<TaskModels.TaskResponse> allTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_task);

        // Đẩy header xuống bằng chiều cao status bar
        View header = findViewById(R.id.headerTaskLayout);
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        prefs      = getSharedPreferences("qlns_pref", MODE_PRIVATE);
        employeeId = prefs.getLong("employeeId", -1);
        role       = prefs.getString("role", "EMPLOYEE");

        apiService = RetrofitClient.getClient().create(ApiService.class);

        recyclerView = findViewById(R.id.recyclerViewTask);
        tabLayout    = findViewById(R.id.tabLayoutTask);
        topAppBar    = findViewById(R.id.topAppBarTask);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new ArrayList<>(), this::onTaskAction);
        recyclerView.setAdapter(adapter);

        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        setupTabs();
        loadTasks();
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterTasks(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterTasks(int tabPos) {
        List<TaskModels.TaskResponse> filtered;
        switch (tabPos) {
            case 1: filtered = allTasks.stream()
                    .filter(t -> "PENDING".equals(t.status))
                    .collect(Collectors.toList()); break;
            case 2: filtered = allTasks.stream()
                    .filter(t -> "ACCEPTED".equals(t.status))
                    .collect(Collectors.toList()); break;
            case 3: filtered = allTasks.stream()
                    .filter(t -> "DONE".equals(t.status))
                    .collect(Collectors.toList()); break;
            default: filtered = new ArrayList<>(allTasks);
        }
        adapter.updateData(filtered);
    }

    private void loadTasks() {
        apiService.getMyTasks(employeeId)
                .enqueue(new Callback<List<TaskModels.TaskResponse>>() {
                    @Override
                    public void onResponse(Call<List<TaskModels.TaskResponse>> call,
                                           Response<List<TaskModels.TaskResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allTasks = response.body();
                            filterTasks(tabLayout.getSelectedTabPosition());
                        }
                    }
                    @Override public void onFailure(Call<List<TaskModels.TaskResponse>> call, Throwable t) {
                        Toast.makeText(TaskActivity.this, "Lỗi tải danh sách công việc", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onTaskAction(TaskModels.TaskResponse task, String action) {
        if ("ACCEPT".equals(action)) {
            apiService.acceptTask(task.id, new TaskModels.AcceptTaskRequest(employeeId))
                    .enqueue(new Callback<TaskModels.TaskResponse>() {
                        @Override
                        public void onResponse(Call<TaskModels.TaskResponse> call,
                                               Response<TaskModels.TaskResponse> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(TaskActivity.this, "Đã nhận việc", Toast.LENGTH_SHORT).show();
                                loadTasks();
                            }
                        }
                        @Override public void onFailure(Call<TaskModels.TaskResponse> call, Throwable t) {}
                    });

        } else if ("DONE".equals(action)) {
            apiService.updateTaskStatus(task.id, employeeId,
                            new TaskModels.UpdateTaskStatusRequest("DONE", "Hoàn thành"))
                    .enqueue(new Callback<TaskModels.TaskResponse>() {
                        @Override
                        public void onResponse(Call<TaskModels.TaskResponse> call,
                                               Response<TaskModels.TaskResponse> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(TaskActivity.this, "Đã đánh dấu hoàn thành", Toast.LENGTH_SHORT).show();
                                loadTasks();
                            }
                        }
                        @Override public void onFailure(Call<TaskModels.TaskResponse> call, Throwable t) {}
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }
}