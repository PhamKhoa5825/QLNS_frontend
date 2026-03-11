package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.Task;
import com.example.myapplication.model.CreateTaskRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.SharedPrefsManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskActivity extends AppCompatActivity {

    private RecyclerView recyclerViewTask;
    private TaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private List<Task> fullTaskList = new ArrayList<>();
    private ImageView btnBackTask;
    private FloatingActionButton fabAddTask;

    private TextView tvFilterAll, tvFilterPending, tvFilterInProgress, tvFilterDone;
    private TextView tvCountPending, tvCountInProgress, tvCountDone;
    
    // Configured for current manager's department
    private Long currentDeptId;
    private Long currentEmployeeId;
    private List<com.example.myapplication.model.Employee> departmentEmployees = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();
        currentEmployeeId = SharedPrefsManager.getInstance(this).getUserId();

        initViews();
        setupRecyclerView();
        setupFilters();
        
        fabAddTask.setOnClickListener(v -> showCreateTaskDialog());
        btnBackTask.setOnClickListener(v -> finish());
        
        fetchTasks();
        fetchDepartmentEmployees();
    }

    private void initViews() {
        recyclerViewTask = findViewById(R.id.recyclerViewTask);
        btnBackTask = findViewById(R.id.btnBackTask);
        fabAddTask = findViewById(R.id.fabAddTask);

        tvFilterAll = findViewById(R.id.tvFilterAll);
        tvFilterPending = findViewById(R.id.tvFilterPending);
        tvFilterInProgress = findViewById(R.id.tvFilterInProgress);
        tvFilterDone = findViewById(R.id.tvFilterDone);

        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountInProgress = findViewById(R.id.tvCountInProgress);
        tvCountDone = findViewById(R.id.tvCountDone);
    }

    private void setupRecyclerView() {
        recyclerViewTask.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this, taskList);
        recyclerViewTask.setAdapter(adapter);
    }

    private void fetchTasks() {
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<List<Task>> call = apiService.getTasksByDepartment(currentDeptId);
        
        call.enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullTaskList = response.body();
                    updateStats();
                    filterTasks("ALL"); // Default show all
                } else {
                    Toast.makeText(TaskActivity.this, "Không thể tải danh sách công việc: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Task>> call, Throwable t) {
                Toast.makeText(TaskActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        int pending = 0, inProgress = 0, done = 0;
        for (Task t : fullTaskList) {
            String s = t.getStatus();
            if (s == null) s = "PENDING";
            
            if (s.equals("PENDING")) pending++;
            else if (s.equals("IN_PROGRESS") || s.equals("ACCEPTED")) inProgress++;
            else if (s.equals("DONE") || s.equals("COMPLETED")) done++;
        }
        tvCountPending.setText(String.valueOf(pending));
        tvCountInProgress.setText(String.valueOf(inProgress));
        tvCountDone.setText(String.valueOf(done));
    }

    private void setupFilters() {
        tvFilterAll.setOnClickListener(v -> filterTasks("ALL"));
        tvFilterPending.setOnClickListener(v -> filterTasks("PENDING"));
        tvFilterInProgress.setOnClickListener(v -> filterTasks("IN_PROGRESS"));
        tvFilterDone.setOnClickListener(v -> filterTasks("DONE"));
    }

    private void filterTasks(String category) {
        // Update UI Tabs
        tvFilterAll.setBackgroundResource(category.equals("ALL") ? R.drawable.bg_tab_selected : 0);
        tvFilterPending.setBackgroundResource(category.equals("PENDING") ? R.drawable.bg_tab_selected : 0);
        tvFilterInProgress.setBackgroundResource(category.equals("IN_PROGRESS") ? R.drawable.bg_tab_selected : 0);
        tvFilterDone.setBackgroundResource(category.equals("DONE") ? R.drawable.bg_tab_selected : 0);

        tvFilterAll.setTypeface(null, category.equals("ALL") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tvFilterPending.setTypeface(null, category.equals("PENDING") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tvFilterInProgress.setTypeface(null, category.equals("IN_PROGRESS") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tvFilterDone.setTypeface(null, category.equals("DONE") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        // Filter List
        List<Task> filtered = new ArrayList<>();
        if (category.equals("ALL")) {
            filtered.addAll(fullTaskList);
        } else {
            for (Task t : fullTaskList) {
                String s = t.getStatus();
                if (s == null) s = "PENDING";

                if (category.equals("PENDING") && s.equals("PENDING")) filtered.add(t);
                else if (category.equals("IN_PROGRESS") && (s.equals("IN_PROGRESS") || s.equals("ACCEPTED"))) filtered.add(t);
                else if (category.equals("DONE") && (s.equals("DONE") || s.equals("COMPLETED"))) filtered.add(t);
            }
        }
        taskList = filtered;
        adapter.setTaskList(taskList);
    }

    private void fetchDepartmentEmployees() {
        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.getEmployeesByDepartmentId(currentDeptId).enqueue(new Callback<List<com.example.myapplication.model.Employee>>() {
            @Override
            public void onResponse(Call<List<com.example.myapplication.model.Employee>> call, Response<List<com.example.myapplication.model.Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    departmentEmployees = response.body();
                }
            }
            @Override
            public void onFailure(Call<List<com.example.myapplication.model.Employee>> call, Throwable t) {}
        });
    }

    private void showCreateTaskDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_task_form);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        EditText edtTitle = dialog.findViewById(R.id.edtTaskTitle);
        EditText edtDesc = dialog.findViewById(R.id.edtTaskDescription);
        Spinner spinnerPriority = dialog.findViewById(R.id.spinnerTaskPriority);
        TextView tvDeadline = dialog.findViewById(R.id.tvTaskDeadline);
        Spinner spinnerAssignees = dialog.findViewById(R.id.spinnerTaskAssignees);
        Button btnSave = dialog.findViewById(R.id.btnSaveTask);

        // 1. Setup Priority Spinner
        String[] priorities = {"LOW", "MEDIUM", "HIGH"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(1); // Default Medium

        // 2. Setup Deadline DatePicker
        tvDeadline.setOnClickListener(v -> {
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String date = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvDeadline.setText(date);
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // 3. Setup Assignees Spinner
        List<String> employeeNames = new ArrayList<>();
        for (com.example.myapplication.model.Employee e : departmentEmployees) {
            employeeNames.add(e.getFullName());
        }
        ArrayAdapter<String> empAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, employeeNames);
        empAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssignees.setAdapter(empAdapter);
        
        btnSave.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String desc = edtDesc.getText().toString().trim();
            String deadline = tvDeadline.getText().toString().trim();
            
            if (title.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                return;
            }
            if (deadline.isEmpty() || deadline.equals("Chọn ngày giờ")) {
                Toast.makeText(this, "Vui lòng chọn hạn chót", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (spinnerAssignees.getSelectedItemPosition() >= 0) {
                CreateTaskRequest req = new CreateTaskRequest();
                req.setTitle(title);
                req.setDescription(desc);
                req.setDeadline(deadline);
                req.setPriority(priorities[spinnerPriority.getSelectedItemPosition()]);
                req.setAssignedToId(departmentEmployees.get(spinnerAssignees.getSelectedItemPosition()).getId());
                req.setAssignedById(currentEmployeeId);

                createTaskAPI(req, dialog);
            } else {
                Toast.makeText(this, "Vui lòng chọn người thực hiện", Toast.LENGTH_SHORT).show();
            }
        });
        
        dialog.show();
    }
    
    private void createTaskAPI(CreateTaskRequest request, Dialog dialog) {
        ApiService apiService = RetrofitClient.getApiService(this);
        Call<Task> call = apiService.createTask(request);
        call.enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if(response.isSuccessful()) {
                    Toast.makeText(TaskActivity.this, "Tạo thành công", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    fetchTasks();
                } else {
                    Toast.makeText(TaskActivity.this, "Tạo thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                Toast.makeText(TaskActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
