package com.example.myapplication.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.dto.TaskDto;
import com.example.myapplication.model.entity.Task;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

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
    private MaterialToolbar topAppBar;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private ApiService apiService;
    private SharedPrefsManager pm;
    private Long employeeId;

    private List<Task> allTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_task);

        // Status bar padding
        View header = findViewById(R.id.headerTaskLayout);
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                v.setPadding(v.getPaddingLeft(), statusBarHeight,
                        v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        pm = SharedPrefsManager.getInstance(this);
        employeeId = pm.getEmployeeId();
        apiService = RetrofitClient.getClient().create(ApiService.class);

        bindViews();
        setupTabs();

        loadTasks();
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.recyclerViewTask);
        tabLayout = findViewById(R.id.tabLayoutTask);
        topAppBar = findViewById(R.id.topAppBarTask);

        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(new ArrayList<>(), this::onTaskAction);
        recyclerView.setAdapter(adapter);

        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                filterByStatus(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // ── DATA ─────────────────────────────────────────────────────

    private void loadTasks() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getMyTasks(employeeId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> c, Response<List<Task>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    allTasks = r.body();
                    filterByStatus(tabLayout.getSelectedTabPosition());
                } else {
                    Toast.makeText(TaskActivity.this,
                            ApiErrorHelper.parse(r, "Lỗi tải nhiệm vụ"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TaskActivity.this, "Lỗi kết nối", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterByStatus(int tabPos) {
        List<Task> filtered;
        switch (tabPos) {
            case 1: filtered = allTasks.stream()
                    .filter(Task::isPending).collect(Collectors.toList()); break;
            case 2: filtered = allTasks.stream()
                    .filter(Task::isAccepted).collect(Collectors.toList()); break;
            case 3: filtered = allTasks.stream()
                    .filter(Task::isDone).collect(Collectors.toList()); break;
            default: filtered = new ArrayList<>(allTasks);
        }
        adapter.updateData(filtered);
        if (tvEmpty != null)
            tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── ACTIONS ──────────────────────────────────────────────────

    private void onTaskAction(Task task, String action) {
        if ("ACCEPT".equals(action)) {
            apiService.acceptTask(task.id, new TaskDto.AcceptTaskRequest(employeeId))
                    .enqueue(new Callback<Task>() {
                        @Override
                        public void onResponse(Call<Task> c, Response<Task> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(TaskActivity.this, "Đã nhận việc", Toast.LENGTH_SHORT).show();
                                loadTasks();
                            } else {
                                Toast.makeText(TaskActivity.this,
                                        ApiErrorHelper.parse(r, "Nhận việc thất bại"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Task> c, Throwable t) {
                            Toast.makeText(TaskActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else if ("DONE".equals(action)) {
            apiService.updateTaskStatus(task.id, employeeId,
                    new TaskDto.UpdateTaskStatusRequest("DONE", "Hoàn thành"))
                    .enqueue(new Callback<Task>() {
                        @Override
                        public void onResponse(Call<Task> c, Response<Task> r) {
                            if (r.isSuccessful()) {
                                Toast.makeText(TaskActivity.this, "Đã đánh dấu hoàn thành", Toast.LENGTH_SHORT).show();
                                loadTasks();
                            } else {
                                Toast.makeText(TaskActivity.this,
                                        ApiErrorHelper.parse(r, "Cập nhật trạng thái thất bại"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Task> c, Throwable t) {
                            Toast.makeText(TaskActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }
}
