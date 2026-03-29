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

import com.example.myapplication.R;
import com.example.myapplication.adapter.EmployeeAdapter;
import com.example.myapplication.model.*;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
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
    private TextView tvHeaderName, tvHeaderDept, tvHeaderAvatarText;
    private View btnHeaderNotifications, btnHeaderExtra, containerProfileLink;
    private ImageView ivHeaderAvatar;

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

        loadDataByRole();
        
        BottomNavHelper.setupBottomNav(this, R.id.nav_people);
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

        // Top Bar
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderDept = findViewById(R.id.tvHeaderDept);
        tvHeaderAvatarText = findViewById(R.id.tvHeaderAvatarText);
        ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
        btnHeaderNotifications = findViewById(R.id.btnHeaderNotifications);
        btnHeaderExtra = findViewById(R.id.btnHeaderExtra);
        containerProfileLink = findViewById(R.id.containerProfileLink);

        setupTopBar();
        
        if (btnHeaderNotifications != null) {
            btnHeaderNotifications.setOnClickListener(v -> startActivity(new android.content.Intent(this, NotificationActivity.class)));
        }
        if (containerProfileLink != null) {
            containerProfileLink.setOnClickListener(v -> startActivity(new android.content.Intent(this, ProfileActivity.class)));
        }

        recyclerView = findViewById(R.id.recyclerViewEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAdapter(new ArrayList<>(), this::showEmployeeDetail);
        recyclerView.setAdapter(adapter);

        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String role = prefs.getRole();
        if ("ADMIN".equals(role)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> showAddEmployeeDialog());
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private void setupTopBar() {
        SharedPrefsManager prefs = SharedPrefsManager.getInstance(this);
        String name = prefs.getFullName();
        String dept = prefs.getDepartmentName();
        
        if (tvHeaderName != null) tvHeaderName.setText(name.isEmpty() ? prefs.getUsername() : name);
        if (tvHeaderDept != null) tvHeaderDept.setText(dept);
        if (tvHeaderAvatarText != null && !name.isEmpty()) {
            tvHeaderAvatarText.setText(String.valueOf(name.charAt(0)).toUpperCase());
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
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> viewModel.searchEmployees(s.toString().trim());
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });

        edtSearch.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (event.getRawX() >= edtSearch.getRight()
                        - edtSearch.getCompoundDrawables()[2].getBounds().width()
                        - edtSearch.getPaddingEnd()) {
                    showFilterDialog();
                    return true;
                }
            }
            return false;
        });
    }

    private void updateStatistics(List<Employee> list) {
        if (tvTotal == null || list == null) return;

        int total = list.size();
        int working = 0;
        for (Employee e : list) {
            if ("ACTIVE".equalsIgnoreCase(e.getStatusRaw())) working++;
        }

        tvTotal.setText("All " + total);
        tvWorking.setText("Online " + working);
        tvResigned.setText("Engineering " + (total - working));
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_employee_sort, null);

        ChipGroup cgRole = view.findViewById(R.id.cgRole);
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
            // Hide department filter for Managers
            View label = view.findViewById(R.id.tvLabelDept);
            if (label != null) label.setVisibility(View.GONE);
            cgDepartment.setVisibility(View.GONE);
        }

        LinkedHashSet<String> positions = new LinkedHashSet<>();
        for (Employee emp : viewModel.getFullEmployeeList()) {
            if (emp.getPosition() != null) positions.add(emp.getPosition());
        }
        for (String pos : positions) addChip(cgRole, pos, pos);

        btnApply.setOnClickListener(v -> {
            String selectedStatus = getSelectedChipTag(cgStatus);
            Long selectedDeptId = getSelectedChipLongTag(cgDepartment);
            String selectedRole = getSelectedChipTag(cgRole);

            viewModel.filterEmployees(selectedStatus, selectedDeptId, selectedRole);
            dialog.dismiss();
        });

        btnReset.setOnClickListener(v -> {
            loadDataByRole();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void addChip(ChipGroup group, String label, String tag) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setTag(tag);
        chip.setCheckable(true);
        group.addView(chip);
    }

    private void addChipWithId(ChipGroup group, String label, Long id) {
        Chip chip = new Chip(this);
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

        ((TextView) view.findViewById(R.id.tvDialogAvatar)).setText(employee.getAvatarText());
        ((TextView) view.findViewById(R.id.tvDialogName)).setText(employee.getFullName());
        ((TextView) view.findViewById(R.id.tvDialogRole)).setText(employee.getRole());

        trySetText(view, R.id.tvDialogEmail, employee.getEmail());
        trySetText(view, R.id.tvDialogPhone, employee.getPhone());
        trySetText(view, R.id.tvDialogDept, employee.getDepartment());
        trySetText(view, R.id.tvDialogStatus, employee.getStatus());
        trySetText(view, R.id.tvDialogJoinDate, employee.getJoinDate());

        // Salary row: only visible to Admin
        View layoutSalaryRow = view.findViewById(R.id.layoutSalaryRow);
        TextView tvDialogSalary = view.findViewById(R.id.tvDialogSalary);
        View btnSetSalary = view.findViewById(R.id.btnSetSalary);
        String role = SharedPrefsManager.getInstance(this).getRole();

        if ("ADMIN".equals(role)) {
            layoutSalaryRow.setVisibility(View.VISIBLE);

            // Display current salary
            Double salary = employee.getBaseSalary();
            if (salary != null && salary > 0) {
                java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                tvDialogSalary.setText(fmt.format(salary.longValue()) + " ₫");
            } else {
                tvDialogSalary.setText("Chưa thiết lập");
            }

            // Set salary button opens an AlertDialog
            btnSetSalary.setOnClickListener(v -> {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Thiết lập lương cơ bản");

                EditText etSalary = new EditText(this);
                etSalary.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                etSalary.setHint("Nhập lương (VD: 15000000)");
                if (salary != null && salary > 0) {
                    etSalary.setText(String.valueOf(salary.longValue()));
                }

                int padPx = (int) (16 * getResources().getDisplayMetrics().density);
                android.widget.FrameLayout container = new android.widget.FrameLayout(this);
                android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(padPx, 0, padPx, 0);
                etSalary.setLayoutParams(params);
                container.addView(etSalary);
                builder.setView(container);

                builder.setPositiveButton("Lưu", (d, which) -> {
                    String input = etSalary.getText().toString().trim();
                    if (input.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập mức lương", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double newSalary = Double.parseDouble(input);
                    updateEmployeeSalary(employee.getId(), newSalary, tvDialogSalary);
                });
                builder.setNegativeButton("Hủy", null);
                builder.show();
            });
        } else {
            layoutSalaryRow.setVisibility(View.GONE);
        }

        view.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
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
        Toast.makeText(this, "Mở màn hình thêm nhân viên", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }
}
