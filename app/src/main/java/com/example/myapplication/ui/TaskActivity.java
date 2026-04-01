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

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.TaskAdapter;
import com.example.myapplication.model.Task;
import com.example.myapplication.model.CreateTaskRequest;
import com.example.myapplication.model.UpdateTaskStatusRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.TopBarHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.bumptech.glide.Glide;

public class TaskActivity extends AppCompatActivity {

    private RecyclerView recyclerViewTask;
    private TaskAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private List<Task> fullTaskList = new ArrayList<>();
    private FloatingActionButton fabAddTask;

    private TextView tvFilterAll, tvFilterPending, tvFilterInProgress, tvFilterDone;
    private TextView tvCountPending, tvCountInProgress, tvCountDone;

    private android.widget.LinearLayout btnMonthFilter;
    private TextView tvSelectedMonth;
    private Integer selectedMonth, selectedYear;
    private EditText edtSearch;
    private String currentSearchKeyword = "";
    private String currentCategory = "ALL";
    
    // Configured for current manager's department
    private Long currentDeptId;
    private Long currentEmployeeId;
    private List<com.example.myapplication.model.Employee> assignableEmployees = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        currentDeptId = SharedPrefsManager.getInstance(this).getDepartmentId();
        currentEmployeeId = SharedPrefsManager.getInstance(this).getEmployeeId();

        java.util.Calendar cal = java.util.Calendar.getInstance();
        selectedMonth = cal.get(java.util.Calendar.MONTH) + 1;
        selectedYear = cal.get(java.util.Calendar.YEAR);

        initViews();
        setupRecyclerView();
        setupFilters();
        
        // Only MANAGER and ADMIN can create tasks
        String role = SharedPrefsManager.getInstance(this).getRole();
        if ("EMPLOYEE".equals(role)) {
            fabAddTask.setVisibility(android.view.View.GONE);
        } else {
            fabAddTask.setOnClickListener(v -> showCreateTaskDialog());
        }
        
        fetchTasks();
        if (!"EMPLOYEE".equals(role)) {
            fetchAssignableEmployees();
        }
        
        BottomNavHelper.setupBottomNav(this, R.id.nav_tasks);
    }
 
    @Override
    protected void onResume() {
        super.onResume();
        TopBarHelper.setupTopBar(this);
    }

    private void initViews() {
        recyclerViewTask = findViewById(R.id.recyclerViewTask);
        fabAddTask = findViewById(R.id.fabAddTask);

        tvFilterAll = findViewById(R.id.tvFilterAll);
        tvFilterPending = findViewById(R.id.tvFilterPending);
        tvFilterInProgress = findViewById(R.id.tvFilterInProgress);
        tvFilterDone = findViewById(R.id.tvFilterDone);

        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountInProgress = findViewById(R.id.tvCountInProgress);
        tvCountDone = findViewById(R.id.tvCountDone);

        btnMonthFilter = findViewById(R.id.btnMonthFilter);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        if (tvSelectedMonth != null) {
            tvSelectedMonth.setText("Tháng " + selectedMonth + "/" + selectedYear);
        }

        edtSearch = findViewById(R.id.edtSearch);
        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchKeyword = s.toString().trim().toLowerCase();
                    filterTasks(currentCategory);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void setupRecyclerView() {
        recyclerViewTask.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this, taskList, this::handleTaskInteraction);
        recyclerViewTask.setAdapter(adapter);
    }

    private void handleTaskInteraction(Task task) {
        showTaskDetailDialog(task);
    }

    private void showTaskDetailDialog(Task task) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_task_detail);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Header
        TextView tvAvatar = dialog.findViewById(R.id.tvDetailAvatar);
        com.google.android.material.imageview.ShapeableImageView ivDetailAvatar = dialog.findViewById(R.id.ivDetailAvatar);
        TextView tvEmployeeName = dialog.findViewById(R.id.tvDetailEmployeeName);
        TextView tvDept = dialog.findViewById(R.id.tvDetailDept);
        
        String name = task.getAssignedToName() != null ? task.getAssignedToName() : "N/A";
        tvEmployeeName.setText(name);
        tvAvatar.setText(name.substring(0, 1).toUpperCase());
        
        if (ivDetailAvatar != null) {
            String avatarUrl = task.getAssignedToAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : RetrofitClient.BASE_URL + avatarUrl;
                Glide.with(this).load(fullUrl).circleCrop().into(ivDetailAvatar);
                ivDetailAvatar.setVisibility(View.VISIBLE);
                tvAvatar.setVisibility(View.GONE);
            } else {
                ivDetailAvatar.setVisibility(View.GONE);
                tvAvatar.setVisibility(View.VISIBLE);
            }
        }
        
        tvDept.setText("Nhiệm vụ cá nhân");

        // Title & Status
        TextView tvTitle = dialog.findViewById(R.id.tvDetailTitle);
        TextView tvStatus = dialog.findViewById(R.id.tvDetailStatus);
        TextView tvPriority = dialog.findViewById(R.id.tvDetailPriority);
        String priority = task.getPriority() != null ? task.getPriority() : "MEDIUM";
        String priorityTag = "TRUNG BÌNH";
        if ("HIGH".equals(priority)) priorityTag = "CAO";
        else if ("LOW".equals(priority)) priorityTag = "THẤP";
        tvPriority.setText(priorityTag);

        // Info
        TextView tvDeadline = dialog.findViewById(R.id.tvDetailDeadline);
        TextView tvCreator = dialog.findViewById(R.id.tvDetailCreator);
        TextView tvDesc = dialog.findViewById(R.id.tvDetailDesc);
        
        tvDeadline.setText(formatDateTime(task.getDeadline()));
        tvCreator.setText(task.getAssignedByName() != null ? task.getAssignedByName() : "Hệ thống");
        tvDesc.setText(task.getDescription());

        // Status Colors
        int statusColor = 0xFF3B82F6; // Blue
        int statusBg = 0xFFDBEAFE;
        String status = task.getStatus() != null ? task.getStatus() : "PENDING";

        // Check Overdue logic
        boolean overdue = isOverdue(task.getDeadline());
        boolean isFinished = "DONE".equals(status) || "COMPLETED".equals(status);
        boolean isSubmitted = "UNDER_REVIEW".equals(status);

        String statusTag = status;
        if (!isFinished && !isSubmitted && overdue) {
            statusTag = "QUÁ HẠN";
            statusColor = 0xFFEF4444; statusBg = 0xFFFEE2E2;
        } else if (isFinished) {
            statusTag = "HOÀN THÀNH";
            statusColor = 0xFF10B981; statusBg = 0xFFD1FAE5;
        } else if ("REJECTED".equals(status)) {
            statusTag = "CẦN LÀM LẠI";
            statusColor = 0xFFEF4444; statusBg = 0xFFFEE2E2;
        } else if ("UNDER_REVIEW".equals(status)) {
            statusTag = "CHỜ DUYỆT";
            statusColor = 0xFFF59E0B; statusBg = 0xFFFEF3C7;
        } else if ("IN_PROGRESS".equals(status) || "ACCEPTED".equals(status)) {
            statusTag = "ĐANG LÀM";
            statusColor = 0xFF8B5CF6; statusBg = 0xFFEDE9FE;
        } else {
            statusTag = "CHỜ NHẬN";
        }
        
        tvStatus.setText(statusTag);
        tvStatus.setTextColor(statusColor);
        tvStatus.getBackground().setTint(statusBg);

        // Priority Colors
        if ("HIGH".equals(task.getPriority())) {
            tvPriority.setTextColor(0xFFEF4444); tvPriority.getBackground().setTint(0xFFFEE2E2);
        } else if ("LOW".equals(task.getPriority())) {
            tvPriority.setTextColor(0xFF6B7280); tvPriority.getBackground().setTint(0xFFF3F4F6);
        }

        // Action Section
        View layoutInput = dialog.findViewById(R.id.layoutDetailActionInput);
        TextView tvLabelInput = dialog.findViewById(R.id.tvLabelActionNote);
        EditText edtNote = dialog.findViewById(R.id.edtDetailNote);
        Button btnPrimary = dialog.findViewById(R.id.btnDetailPrimaryAction);
        Button btnSecondary = dialog.findViewById(R.id.btnDetailSecondaryAction);
        View btnCloseX = dialog.findViewById(R.id.btnModalClose);

        String role = SharedPrefsManager.getInstance(this).getRole();
        boolean isEmp = role != null && role.toUpperCase().contains("EMPLOYEE");
        boolean isMan = role != null && (role.toUpperCase().contains("MANAGER") || role.toUpperCase().contains("ADMIN"));

        if (isEmp) {
            if ("PENDING".equals(status)) {
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("Nhận nhiệm vụ");
                btnPrimary.setOnClickListener(v -> {
                    dialog.dismiss();
                    acceptTaskAPI(task);
                });
            } else if ("ACCEPTED".equals(status) || "IN_PROGRESS".equals(status) || "REJECTED".equals(status)) {
                layoutInput.setVisibility(View.VISIBLE);
                tvLabelInput.setText("Báo cáo hoàn thành:");
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("Gửi duyệt");
                btnPrimary.setOnClickListener(v -> {
                    String note = edtNote.getText().toString().trim();
                    updateTaskStatusAPI(task, "DONE", note, dialog);
                });
            }
        } else if (isMan) {
            if ("UNDER_REVIEW".equals(status)) {
                layoutInput.setVisibility(View.VISIBLE);
                tvLabelInput.setText("Nhận xét:");
                btnPrimary.setVisibility(View.VISIBLE);
                btnPrimary.setText("Duyệt");
                btnPrimary.setOnClickListener(v -> updateTaskStatusAPI(task, "DONE", edtNote.getText().toString().trim(), dialog));
                
                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setText("Yêu cầu sửa lại");
                btnSecondary.setOnClickListener(v -> updateTaskStatusAPI(task, "REJECTED", edtNote.getText().toString().trim(), dialog));
            }
        }

        dialog.findViewById(R.id.btnDetailClose).setOnClickListener(v -> dialog.dismiss());
        if (btnCloseX != null) btnCloseX.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean isOverdue(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isEmpty()) return false;
        try {
            java.text.SimpleDateFormat sdf;
            if (deadlineStr.contains("T")) {
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            } else {
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            }
            java.util.Date deadline = sdf.parse(deadlineStr);
            return deadline != null && deadline.before(new java.util.Date());
        } catch (Exception e) {
            return false;
        }
    }

    private String formatDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "--/--/----";
        try {
            if (dateStr.contains("T")) {
                String[] parts = dateStr.split("T");
                String[] dateParts = parts[0].split("-");
                String[] timeParts = parts[1].split(":");
                String formattedDate = dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];
                String formattedTime = timeParts[0] + ":" + timeParts[1];
                return formattedTime + " - " + formattedDate;
            } else if (dateStr.contains("-")) {
                String[] dateParts = dateStr.split("-");
                if(dateParts.length >= 3) {
                   return dateParts[2] + "/" + dateParts[1] + "/" + dateParts[0];
                }
            }
        } catch (Exception e) {}
        return dateStr;
    }



    private void acceptTaskAPI(Task task) {
        ApiService apiService = RetrofitClient.getApiService(this);
        java.util.Map<String, Long> body = new java.util.HashMap<>();
        body.put("employeeId", SharedPrefsManager.getInstance(this).getEmployeeId());

        apiService.acceptTask(task.getId(), body).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TaskActivity.this, "Đã nhận nhiệm vụ", Toast.LENGTH_SHORT).show();
                    fetchTasks();
                } else {
                    Toast.makeText(TaskActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Task> call, Throwable t) {
                Toast.makeText(TaskActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTaskStatusAPI(Task task, String status, String note, Dialog dialog) {
        ApiService apiService = RetrofitClient.getApiService(this);
        UpdateTaskStatusRequest req = new UpdateTaskStatusRequest(status, note);
        Long empId = SharedPrefsManager.getInstance(this).getEmployeeId();

        apiService.updateTaskStatus(task.getId(), req, empId).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TaskActivity.this, "Thành công", Toast.LENGTH_SHORT).show();
                    if (dialog != null) dialog.dismiss();
                    fetchTasks();
                } else {
                    Toast.makeText(TaskActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Task> call, Throwable t) {
                Toast.makeText(TaskActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTasks() {
        ApiService apiService = RetrofitClient.getApiService(this);
        String role = SharedPrefsManager.getInstance(this).getRole();
        
        Call<List<Task>> call;
        if ("ADMIN".equals(role)) {
            // Admin xem tất cả tasks
            call = apiService.getAllTasks();
        } else if ("EMPLOYEE".equals(role)) {
            // Employee xem tasks của mình
            Long empId = SharedPrefsManager.getInstance(this).getEmployeeId();
            call = apiService.getMyTasks(empId);
        } else {
            // Manager xem tasks theo phòng ban
            call = apiService.getTasksByDepartment(currentDeptId);
        }
        
        call.enqueue(new Callback<List<Task>>() {
            @Override
            public void onResponse(Call<List<Task>> call, Response<List<Task>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullTaskList = response.body();
                    updateStats();
                    filterTasks("ALL"); // Default show all
                } else {
                    String errorMsg = "Lỗi " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyStr = response.errorBody().string();
                            org.json.JSONObject jsonObj = new org.json.JSONObject(errorBodyStr);
                            if (jsonObj.has("message")) {
                                errorMsg += ": " + jsonObj.getString("message");
                            } else {
                                errorMsg += ": " + errorBodyStr;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(TaskActivity.this, "Không thể tải danh sách công việc: " + errorMsg, Toast.LENGTH_LONG).show();
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
        String targetPrefix = (selectedMonth != null && selectedMonth > 0) ? String.format(java.util.Locale.US, "%04d-%02d", selectedYear, selectedMonth) : null;

        for (Task t : fullTaskList) {
            String s = t.getStatus();
            if (s == null) s = "PENDING";
            
            // Determine if date matches
            boolean matchesMonth = true;
            if (targetPrefix != null) {
                String dateToCheck = t.getCreatedAt() != null ? t.getCreatedAt() : t.getDeadline();
                if (dateToCheck != null && !dateToCheck.startsWith(targetPrefix)) {
                    matchesMonth = false;
                } else if (dateToCheck == null) {
                    matchesMonth = false;
                }
            }

            if (matchesMonth) {
                if (s.equals("PENDING")) pending++;
                else if (s.equals("IN_PROGRESS") || s.equals("ACCEPTED") || s.equals("UNDER_REVIEW") || s.equals("REJECTED")) inProgress++;
                else if (s.equals("DONE") || s.equals("COMPLETED")) done++;
            }
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
        
        if (btnMonthFilter != null) {
            btnMonthFilter.setOnClickListener(v -> showMonthPicker());
        }
    }

    private void showMonthPicker() {
        String[] months = new String[13];
        months[0] = "Xem toàn bộ thời gian";
        for (int i = 0; i < 12; i++) months[i+1] = "Tháng " + (i + 1);
        
        new android.app.AlertDialog.Builder(this)
            .setTitle("Chọn tháng")
            .setItems(months, (dialog, which) -> {
                if (which == 0) {
                    selectedMonth = 0; 
                    if (tvSelectedMonth != null) tvSelectedMonth.setText("Toàn thời gian");
                } else {
                    selectedMonth = which;
                    if (tvSelectedMonth != null) tvSelectedMonth.setText("Tháng " + selectedMonth + "/" + selectedYear);
                }
                filterTasks(currentCategory);
            })
            .show();
    }

    private void filterTasks(String category) {
        currentCategory = category;
        // Update UI Tabs
        int unselectedBg = R.drawable.bg_chip_unselected;
        int selectedBg = R.drawable.bg_chip_selected;
        int unselectedTextColor = getResources().getColor(R.color.secondary);
        int selectedTextColor = android.graphics.Color.WHITE;

        tvFilterAll.setBackgroundResource(category.equals("ALL") ? selectedBg : unselectedBg);
        tvFilterPending.setBackgroundResource(category.equals("PENDING") ? selectedBg : unselectedBg);
        tvFilterInProgress.setBackgroundResource(category.equals("IN_PROGRESS") ? selectedBg : unselectedBg);
        tvFilterDone.setBackgroundResource(category.equals("DONE") ? selectedBg : unselectedBg);

        tvFilterAll.setTextColor(category.equals("ALL") ? selectedTextColor : unselectedTextColor);
        tvFilterPending.setTextColor(category.equals("PENDING") ? selectedTextColor : unselectedTextColor);
        tvFilterInProgress.setTextColor(category.equals("IN_PROGRESS") ? selectedTextColor : unselectedTextColor);
        tvFilterDone.setTextColor(category.equals("DONE") ? selectedTextColor : unselectedTextColor);

        tvFilterAll.setTypeface(null, category.equals("ALL") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tvFilterPending.setTypeface(null, category.equals("PENDING") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tvFilterInProgress.setTypeface(null, category.equals("IN_PROGRESS") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tvFilterDone.setTypeface(null, category.equals("DONE") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        // Filter List
        List<Task> filtered = new ArrayList<>();
        String targetPrefix = (selectedMonth != null && selectedMonth > 0) ? String.format(java.util.Locale.US, "%04d-%02d", selectedYear, selectedMonth) : null;

        for (Task t : fullTaskList) {
            String s = t.getStatus();
            if (s == null) s = "PENDING";

            // 1. Month Filter
            boolean matchesMonth = true;
            if (targetPrefix != null) {
                String dateToCheck = t.getCreatedAt() != null ? t.getCreatedAt() : t.getDeadline();
                if (dateToCheck != null && !dateToCheck.startsWith(targetPrefix)) {
                    matchesMonth = false;
                } else if (dateToCheck == null) {
                    matchesMonth = false;
                }
            }

            // 2. Keyword Filter
            boolean matchesKeyword = true;
            if (currentSearchKeyword != null && !currentSearchKeyword.isEmpty()) {
                String title = t.getTitle() != null ? t.getTitle().toLowerCase() : "";
                String desc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
                if (!title.contains(currentSearchKeyword) && !desc.contains(currentSearchKeyword)) {
                    matchesKeyword = false;
                }
            }

            if (matchesMonth && matchesKeyword) {
                if (category.equals("ALL")) {
                    filtered.add(t);
                } else if (category.equals("PENDING") && s.equals("PENDING")) {
                    filtered.add(t);
                } else if (category.equals("IN_PROGRESS") && (s.equals("IN_PROGRESS") || s.equals("ACCEPTED") || s.equals("UNDER_REVIEW") || s.equals("REJECTED"))) {
                    filtered.add(t);
                } else if (category.equals("DONE") && (s.equals("DONE") || s.equals("COMPLETED"))) {
                    filtered.add(t);
                }
            }
        }
        
        updateStats(); // Ensure stats reflect the newly selected month filter
        taskList = filtered;
        adapter.setTaskList(taskList);
    }

    private void fetchAssignableEmployees() {
        ApiService apiService = RetrofitClient.getApiService(this);
        String role = SharedPrefsManager.getInstance(this).getRole();
        
        Call<List<com.example.myapplication.model.Employee>> call;
        if ("ADMIN".equals(role)) {
            call = apiService.getEmployees();
        } else {
            call = apiService.getEmployeesByDepartmentId(currentDeptId);
        }
        
        call.enqueue(new Callback<List<com.example.myapplication.model.Employee>>() {
            @Override
            public void onResponse(Call<List<com.example.myapplication.model.Employee>> call, Response<List<com.example.myapplication.model.Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<com.example.myapplication.model.Employee> raw = response.body();
                    List<com.example.myapplication.model.Employee> filtered = new ArrayList<>();
                    for (com.example.myapplication.model.Employee e : raw) {
                        // 1. Cannot assign to self (Manager or Admin)
                        if (e.getId() != null && e.getId().equals(currentEmployeeId)) continue;

                        // 2. Admin cannot assign to other Admins
                        if ("ADMIN".equals(role)) {
                            if ("ADMIN".equalsIgnoreCase(e.getRoleRaw())) continue;
                        }
                        filtered.add(e);
                    }
                    assignableEmployees = filtered;
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
        View btnCancel = dialog.findViewById(R.id.btnCancelTask);
        View btnClose = dialog.findViewById(R.id.btnModalClose);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

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
        for (com.example.myapplication.model.Employee e : assignableEmployees) {
            String position = e.getPosition() != null ? e.getPosition() : "";
            employeeNames.add(e.getFullName() + (position.isEmpty() ? "" : " - " + position));
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
                req.setAssignedToId(assignableEmployees.get(spinnerAssignees.getSelectedItemPosition()).getId());
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
