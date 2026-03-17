package com.example.myapplication.ui;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.TaskEmployeeAdapter;
import com.example.myapplication.model.Task;
import com.example.myapplication.model.UpdateTaskStatusRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskManagementActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private TaskEmployeeAdapter adapter;
    private List<Task> currentList = new ArrayList<>();
    private List<Task> fullList = new ArrayList<>();
    
    private LinearLayout tabPending, tabInProgress, tabDone;
    private TextView tvTabPendingLabel, tvTabInProgressLabel, tvTabDoneLabel;
    private TextView tvCountPending, tvCountInProgress, tvCountDone;
    private ProgressBar progressBar;
    
    private ApiService apiService;
    private Long currentEmployeeId;
    private String currentCategory = "PENDING"; // Default tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_management);

        apiService = RetrofitClient.getApiService();
        currentEmployeeId = SharedPrefsManager.getInstance(this).getEmployeeId();

        initViews();
        setupRecyclerView();
        setupListeners();
        
        fetchTasks();
    }

    private void initViews() {
        rvTasks = findViewById(R.id.rvTasks);
        tabPending = findViewById(R.id.tabPending);
        tabInProgress = findViewById(R.id.tabInProgress);
        tabDone = findViewById(R.id.tabDone);
        
        tvTabPendingLabel = findViewById(R.id.tvTabPendingLabel);
        tvTabInProgressLabel = findViewById(R.id.tvTabInProgressLabel);
        tvTabDoneLabel = findViewById(R.id.tvTabDoneLabel);
        
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountInProgress = findViewById(R.id.tvCountInProgress);
        tvCountDone = findViewById(R.id.tvCountDone);
        
        progressBar = findViewById(R.id.progressBar);
        
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskEmployeeAdapter(this, currentList, this::handleTaskClick);
        rvTasks.setAdapter(adapter);
    }

    private void setupListeners() {
        tabPending.setOnClickListener(v -> filterTasks("PENDING"));
        tabInProgress.setOnClickListener(v -> filterTasks("IN_PROGRESS"));
        tabDone.setOnClickListener(v -> filterTasks("DONE"));
    }

    private void fetchTasks() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getMyTasks(currentEmployeeId).enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    fullList = response.body();
                    updateCounts();
                    filterTasks(currentCategory);
                } else {
                    Toast.makeText(TaskManagementActivity.this, "Không thể tải danh sách nhiệm vụ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TaskManagementActivity.this, "Lỗi kết nối server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCounts() {
        int pending = 0, inProgress = 0, done = 0;
        for (Task t : fullList) {
            String status = t.getStatus() != null ? t.getStatus() : "PENDING";
            if ("PENDING".equals(status)) pending++;
            else if ("ACCEPTED".equals(status) || "IN_PROGRESS".equals(status)) inProgress++;
            else if ("DONE".equals(status) || "COMPLETED".equals(status)) done++;
        }
        tvCountPending.setText("(" + pending + ")");
        tvCountInProgress.setText("(" + inProgress + ")");
        tvCountDone.setText("(" + done + ")");
    }

    private void filterTasks(String category) {
        currentCategory = category;
        updateTabUI(category);
        
        List<Task> filtered = new ArrayList<>();
        for (Task t : fullList) {
            String status = t.getStatus() != null ? t.getStatus() : "PENDING";
            if ("PENDING".equals(category) && "PENDING".equals(status)) {
                filtered.add(t);
            } else if ("IN_PROGRESS".equals(category) && ("ACCEPTED".equals(status) || "IN_PROGRESS".equals(status))) {
                filtered.add(t);
            } else if ("DONE".equals(category) && ("DONE".equals(status) || "COMPLETED".equals(status))) {
                filtered.add(t);
            }
        }
        currentList = filtered;
        adapter.setTaskList(currentList);
    }

    private void updateTabUI(String category) {
        // Reset all
        tabPending.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabInProgress.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabDone.setBackgroundResource(R.drawable.bg_tab_unselected);
        
        tvTabPendingLabel.setTextColor(Color.parseColor("#333333"));
        tvCountPending.setTextColor(Color.parseColor("#888888"));
        tvTabInProgressLabel.setTextColor(Color.parseColor("#333333"));
        tvCountInProgress.setTextColor(Color.parseColor("#888888"));
        tvTabDoneLabel.setTextColor(Color.parseColor("#333333"));
        tvCountDone.setTextColor(Color.parseColor("#888888"));

        // Set active
        if ("PENDING".equals(category)) {
            tabPending.setBackgroundResource(R.drawable.bg_tab_selected);
            tvTabPendingLabel.setTextColor(Color.WHITE);
            tvCountPending.setTextColor(Color.WHITE);
        } else if ("IN_PROGRESS".equals(category)) {
            tabInProgress.setBackgroundResource(R.drawable.bg_tab_selected);
            tvTabInProgressLabel.setTextColor(Color.WHITE);
            tvCountInProgress.setTextColor(Color.WHITE);
        } else if ("DONE".equals(category)) {
            tabDone.setBackgroundResource(R.drawable.bg_tab_selected);
            tvTabDoneLabel.setTextColor(Color.WHITE);
            tvCountDone.setTextColor(Color.WHITE);
        }
    }

    private void handleTaskClick(Task task) {
        String status = task.getStatus() != null ? task.getStatus() : "PENDING";
        if ("PENDING".equals(status)) {
            showAcceptDialog(task);
        } else if ("ACCEPTED".equals(status) || "IN_PROGRESS".equals(status)) {
            showDoneDialog(task);
        }
    }

    private void showAcceptDialog(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Nhận nhiệm vụ")
                .setMessage("Bạn có muốn nhận nhiệm vụ '" + task.getTitle() + "' không?")
                .setPositiveButton("Nhận", (dialog, which) -> acceptTask(task))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void acceptTask(Task task) {
        Map<String, Long> body = new HashMap<>();
        body.put("employeeId", currentEmployeeId);
        
        apiService.acceptTask(task.getId(), body).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TaskManagementActivity.this, "Đã nhận nhiệm vụ", Toast.LENGTH_SHORT).show();
                    fetchTasks();
                } else {
                    Toast.makeText(TaskManagementActivity.this, "Nhận thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                Toast.makeText(TaskManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDoneDialog(Task task) {
        Dialog dialog = new Dialog(this);
        // Use a simple custom layout for the done dialog if it exists, otherwise use a generic input dialog
        // For simplicity and since I haven't created a specific dialog xml, I'll use a prompt
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_task_form, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialogView.findViewById(R.id.tvTaskFormTitle);
        if (tvTitle != null) tvTitle.setText("Hoàn thành nhiệm vụ");

        EditText edtNote = dialogView.findViewById(R.id.edtTaskDescription);
        if (edtNote != null) {
            edtNote.setHint("Nhập ghi chú hoàn thành...");
        }

        // Hide unnecessary fields from dialog_task_form
        if (dialogView.findViewById(R.id.edtTaskTitle) != null) dialogView.findViewById(R.id.edtTaskTitle).setVisibility(View.GONE);
        if (dialogView.findViewById(R.id.spinnerTaskPriority) != null) dialogView.findViewById(R.id.spinnerTaskPriority).setVisibility(View.GONE);
        if (dialogView.findViewById(R.id.tvTaskDeadline) != null) dialogView.findViewById(R.id.tvTaskDeadline).setVisibility(View.GONE);
        if (dialogView.findViewById(R.id.spinnerTaskAssignees) != null) dialogView.findViewById(R.id.spinnerTaskAssignees).setVisibility(View.GONE);

        Button btnSave = dialogView.findViewById(R.id.btnSaveTask);
        if (btnSave != null) {
            btnSave.setText("Hoàn thành");
            btnSave.setOnClickListener(v -> {
                String note = edtNote != null ? edtNote.getText().toString() : "";
                updateTaskStatus(task, "DONE", note, dialog);
            });
        }
        
        dialog.show();
    }

    private void updateTaskStatus(Task task, String status, String note, Dialog dialog) {
        UpdateTaskStatusRequest req = new UpdateTaskStatusRequest(status, note);
        apiService.updateTaskStatus(task.getId(), req, currentEmployeeId).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TaskManagementActivity.this, "Đã hoàn thành nhiệm vụ", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    fetchTasks();
                } else {
                    Toast.makeText(TaskManagementActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                Toast.makeText(TaskManagementActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
