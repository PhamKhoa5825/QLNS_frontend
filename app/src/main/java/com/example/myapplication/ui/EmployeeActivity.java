package com.example.myapplication.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.adapter.EmployeeAdapter;
import com.example.myapplication.model.*;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.TopBarHelper;
import com.example.myapplication.viewmodel.EmployeeViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class EmployeeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private ProgressBar progressBar;
    private EditText edtSearch;
    private FloatingActionButton fabAdd;
    private TextView tvTotal, tvWorking, tvResigned;
    private View btnHeaderAdd;

    private EmployeeViewModel viewModel;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private static final String PREF_NAME = "qlns_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee);

        viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);

        initViews();
        setupSearch();
        observeViewModel();
        BottomNavHelper.setupBottomNav(this, R.id.nav_people);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TopBarHelper.setupTopBar(this);
        loadDataByRole();
    }

    private void loadDataByRole() {
        // Backend handles filtering automatically based on JWT role (Admin sees all, Manager sees their dept)
        viewModel.loadEmployees();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        edtSearch   = findViewById(R.id.edtSearch);
        fabAdd      = findViewById(R.id.fabAddEmployee);
        tvTotal     = findViewById(R.id.tvTotal);
        tvWorking   = findViewById(R.id.tvWorking);
        tvResigned  = findViewById(R.id.tvResigned);

        btnHeaderAdd = findViewById(R.id.btnHeaderAdd);
        View btnFilter = findViewById(R.id.btnFilter);

        recyclerView = findViewById(R.id.recyclerViewEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAdapter(new ArrayList<>(), this::showEmployeeDetail);
        recyclerView.setAdapter(adapter);

        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }

        // Setup top stat chips
        if (tvTotal != null) tvTotal.setOnClickListener(v -> {
            updateStatSelection(tvTotal);
            viewModel.filterEmployees(null, null, null);
        });
        if (tvWorking != null) tvWorking.setOnClickListener(v -> {
            updateStatSelection(tvWorking);
            viewModel.filterEmployees("ACTIVE", null, null);
        });
        if (tvResigned != null) tvResigned.setOnClickListener(v -> {
            updateStatSelection(tvResigned);
            viewModel.filterEmployees("RESIGNED", null, null);
        });

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String role = prefs.getRole();
        if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> showAddEmployeeDialog());
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        viewModel.employees.observe(this, list -> {
            adapter.setData(list);
            updateStatistics(list);
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.errorMessage.observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                // Instant local search
                viewModel.searchEmployees(s.toString().trim());
            }
        });
    }

    private void updateStatistics(List<Employee> list) {
        if (tvTotal == null || list == null) return;

        int total = list.size();
        int working = 0;
        for (Employee e : list) {
            if ("ACTIVE".equalsIgnoreCase(e.getStatusRaw())) working++;
        }

        tvTotal.setText("Tất cả: " + total);
        tvWorking.setText("Đang làm việc: " + working);
        tvResigned.setText("Đã nghỉ việc: " + (total - working));
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_employee_sort, null);

        ChipGroup cgDepartment = view.findViewById(R.id.cgDepartment);
        ChipGroup cgStatus = view.findViewById(R.id.cgStatus);
        Button btnApply = view.findViewById(R.id.btnApply);
        Button btnReset = view.findViewById(R.id.btnReset);

        addChip(cgStatus, "Đang làm việc", "ACTIVE");
        addChip(cgStatus, "Đã nghỉ việc", "RESIGNED");

        String currentRole = SharedPrefsManager.getInstance(this).getRole();
        if ("ADMIN".equals(currentRole)) {
            view.findViewById(R.id.tvLabelDept).setVisibility(View.VISIBLE);
            cgDepartment.setVisibility(View.VISIBLE);
            viewModel.departments.observe(this, departments -> {
                cgDepartment.removeAllViews();
                if (departments != null) {
                    for (Department dept : departments) {
                        addChipWithId(cgDepartment, dept.getName(), dept.getId());
                    }
                }
            });
            viewModel.loadDepartments();
        } else {
            View label = view.findViewById(R.id.tvLabelDept);
            if (label != null) label.setVisibility(View.GONE);
            cgDepartment.setVisibility(View.GONE);
        }

        btnApply.setOnClickListener(v -> {
            String selectedStatus = getSelectedChipTag(cgStatus);
            Long selectedDeptId = getSelectedChipLongTag(cgDepartment);

            viewModel.filterEmployees(selectedStatus, selectedDeptId, null);
            dialog.dismiss();
        });

        btnReset.setOnClickListener(v -> {
            viewModel.filterEmployees(null, null, null);
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void addChip(ChipGroup group, String label, String tag) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(tag);
        chip.setCheckable(true);
        group.addView(chip);
    }

    private void addChipWithId(ChipGroup group, String label, Long id) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(id);
        chip.setCheckable(true);
        group.addView(chip);
    }

    private String getSelectedChipTag(ChipGroup group) {
        int id = group.getCheckedChipId();
        return id != -1 ? (String) group.findViewById(id).getTag() : null;
    }

    private Long getSelectedChipLongTag(ChipGroup group) {
        int id = group.getCheckedChipId();
        return id != -1 ? (Long) group.findViewById(id).getTag() : null;
    }

    private void showEmployeeDetail(Employee employee) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_employee_detail, null);

        // Bind Basic Info
        TextView tvAvatar = view.findViewById(R.id.tvDialogAvatar);
        ImageView ivAvatar = view.findViewById(R.id.ivDialogAvatar);
        TextView tvName = view.findViewById(R.id.tvDialogName);
        TextView tvRole = view.findViewById(R.id.tvDialogRole);
        TextView tvEmail = view.findViewById(R.id.tvDialogEmail);
        TextView tvPhone = view.findViewById(R.id.tvDialogPhone);
        TextView tvDept = view.findViewById(R.id.tvDialogDept);
        TextView tvStatus = view.findViewById(R.id.tvDialogStatus);
        TextView tvJoinDate = view.findViewById(R.id.tvDialogJoinDate);
 
        String avatarUrl = employee.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (ivAvatar != null) ivAvatar.setVisibility(View.VISIBLE);
            if (tvAvatar != null) tvAvatar.setVisibility(View.GONE);
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .into(ivAvatar);
        } else {
            if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
            if (tvAvatar != null) {
                tvAvatar.setVisibility(View.VISIBLE);
                tvAvatar.setText(employee.getAvatarText());
            }
        }
        if (tvName != null) tvName.setText(employee.getFullName());
        if (tvRole != null) tvRole.setText(employee.getPosition());
        if (tvEmail != null) tvEmail.setText(employee.getEmail());
        if (tvPhone != null) tvPhone.setText(employee.getPhone());
        if (tvDept != null) tvDept.setText(employee.getDepartmentName());
        if (tvStatus != null) tvStatus.setText(employee.getStatus());
        if (tvJoinDate != null) tvJoinDate.setText(employee.getJoinDate());

        // View Details (Full Screen)
        Button btnViewDetail = view.findViewById(R.id.btnViewDetail);
        if (btnViewDetail != null) {
            btnViewDetail.setOnClickListener(v -> {
                dialog.dismiss();
                android.content.Intent intent = new android.content.Intent(this, EmployeeDetailActivity.class);
                intent.putExtra("employeeId", employee.getId());
                startActivity(intent);
            });
        }

        // Close Button
        View btnClose = view.findViewById(R.id.btnCloseDialog);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Admin Actions
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String currentRole = prefs.getRole();
        Button btnEdit = view.findViewById(R.id.btnEditEmployeeQuick);

        if ("ADMIN".equals(currentRole) || "MANAGER".equals(currentRole)) {
            if (btnEdit != null) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    dialog.dismiss();
                    android.content.Intent intent = new android.content.Intent(this, AddEditEmployeeActivity.class);
                    intent.putExtra("employeeId", employee.getId());
                    startActivity(intent);
                });
            }
        } else {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
        }

        dialog.setContentView(view);
        dialog.show();
    }

    private void confirmResign(Employee emp) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận nghỉ việc")
                .setMessage("Cho nhân viên " + emp.getFullName() + " nghỉ việc? Thao tác này sẽ cập nhật trạng thái nhân viên thành Đã nghỉ việc.")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    com.example.myapplication.network.RetrofitClient.getApiService(this)
                            .resignEmployee(emp.getId())
                            .enqueue(new retrofit2.Callback<Void>() {
                                @Override
                                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(EmployeeActivity.this, "Đã cập nhật trạng thái nghỉ việc", Toast.LENGTH_SHORT).show();
                                        viewModel.loadEmployees(); // Reload list
                                    } else {
                                        Toast.makeText(EmployeeActivity.this, "Lỗi khi xử lý nghỉ việc", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                                    Toast.makeText(EmployeeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }


    private void updateEmployeeSalary(Long empId, double salary, TextView tvToUpdate) {
        com.example.myapplication.network.ApiService apiService =
                com.example.myapplication.network.RetrofitClient.getApiService(this);

        java.util.Map<String, Double> body = new java.util.HashMap<>();
        body.put("baseSalary", salary);

        apiService.updateEmployeeBaseSalary(empId, body).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                    tvToUpdate.setText(fmt.format((long) salary) + " ₫");
                    Toast.makeText(EmployeeActivity.this, "✅ Đã lưu lương thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EmployeeActivity.this, "Lỗi khi lưu lương", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Toast.makeText(EmployeeActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void trySetText(View parent, int viewId, String text) {
        TextView v = parent.findViewById(viewId);
        if (v != null && text != null) v.setText(text);
    }

    private void showAddEmployeeDialog() {
        android.content.Intent intent = new android.content.Intent(this, AddEditEmployeeActivity.class);
        startActivity(intent);
    }

    private void updateStatSelection(TextView selectedView) {
        TextView[] views = {tvTotal, tvWorking, tvResigned};
        for (TextView v : views) {
            if (v == null) continue;
            if (v == selectedView) {
                v.setBackgroundResource(R.drawable.bg_chip_selected);
                v.setTextColor(getResources().getColor(R.color.white));
            } else {
                v.setBackgroundResource(R.drawable.bg_chip_unselected);
                v.setTextColor(getResources().getColor(R.color.secondary));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }
}
