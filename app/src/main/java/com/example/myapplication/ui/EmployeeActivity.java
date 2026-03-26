package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.EmployeeAdapter;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.viewmodel.EmployeeViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private ProgressBar progressBar;
    private EditText edtSearch;
    private ImageView btnSort;
    private TextView tvFilterInfo;
    private FloatingActionButton fabAdd;
    private TextView tvTotal, tvWorking, tvResigned;

    private EmployeeViewModel viewModel;
    private ApiService apiService;
    private String role;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private String filterStatus = null;
    private Long filterDeptId = null;
    private String filterDeptName = null;
    private String filterPosition = null;

    private List<Department> cachedDepartments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Status bar trong suốt, trùng màu toolbar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_employee);

        viewModel  = new ViewModelProvider(this).get(EmployeeViewModel.class);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        role       = getSharedPreferences("qlns_pref", MODE_PRIVATE).getString("role", "EMPLOYEE");

        initViews();
        setupSearch();
        observeViewModel();
        handleIntentFilter();

        viewModel.loadEmployees();
        viewModel.loadDepartments();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        progressBar  = findViewById(R.id.progressBar);
        edtSearch    = findViewById(R.id.edtSearch);
        btnSort      = findViewById(R.id.btnSort);
        tvFilterInfo = findViewById(R.id.tvFilterInfo);
        fabAdd       = findViewById(R.id.fabAddEmployee);
        tvTotal      = findViewById(R.id.tvTotal);
        tvWorking    = findViewById(R.id.tvWorking);
        tvResigned   = findViewById(R.id.tvResigned);

        recyclerView = findViewById(R.id.recyclerViewEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeAdapter(new ArrayList<>(), this::showEmployeeDetail);
        recyclerView.setAdapter(adapter);

        btnSort.setOnClickListener(v -> showSortDialog());

        if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v ->
                    startActivity(new Intent(this, AddEditEmployeeActivity.class)));
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private void handleIntentFilter() {
        Intent intent = getIntent();
        if (intent.hasExtra("filterDeptId")) {
            filterDeptId = intent.getLongExtra("filterDeptId", -1);
            filterDeptName = intent.getStringExtra("filterDeptName");
            if (filterDeptId == -1) filterDeptId = null;
            updateFilterInfoText();
        }
    }

    private void observeViewModel() {
        viewModel.employees.observe(this, list -> {
            if (hasActiveFilter()) {
                applyLocalFilter(list);
            } else {
                adapter.setData(list);
                updateStats(list);
            }
        });
        viewModel.isLoading.observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
        viewModel.errorMessage.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        viewModel.departments.observe(this, depts -> {
            if (depts != null) cachedDepartments = depts;
        });
    }

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    String keyword = s.toString().trim();
                    if (keyword.isEmpty() && hasActiveFilter()) {
                        viewModel.filterEmployees(filterStatus, filterDeptId, filterPosition);
                    } else {
                        viewModel.searchEmployees(keyword);
                    }
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });
    }

    // ── SORT / FILTER DIALOG ──────────────────────────────────────

    private void showSortDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_employee_sort, null);

        ChipGroup cgRole   = view.findViewById(R.id.cgRole);
        ChipGroup cgDept   = view.findViewById(R.id.cgDepartment);
        ChipGroup cgStatus = view.findViewById(R.id.cgStatus);
        MaterialButton btnReset = view.findViewById(R.id.btnReset);
        MaterialButton btnApply = view.findViewById(R.id.btnApply);

        addChip(cgStatus, "Đang làm việc", "ACTIVE",
                "ACTIVE".equals(filterStatus));
        addChip(cgStatus, "Đã nghỉ việc", "RESIGNED",
                "RESIGNED".equals(filterStatus));

        for (Department dept : cachedDepartments) {
            addChip(cgDept, dept.getName(), String.valueOf(dept.getId()),
                    filterDeptId != null && filterDeptId.equals(dept.getId()));
        }

        Set<String> positions = new HashSet<>();
        for (Employee emp : viewModel.getFullEmployeeList()) {
            if (emp.getPosition() != null && !emp.getPosition().isEmpty()) {
                positions.add(emp.getPosition());
            }
        }
        for (String pos : positions) {
            addChip(cgRole, pos, pos, pos.equals(filterPosition));
        }

        btnReset.setOnClickListener(v -> {
            filterStatus = null;
            filterDeptId = null;
            filterDeptName = null;
            filterPosition = null;
            updateFilterInfoText();
            viewModel.loadEmployees();
            dialog.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            filterStatus = getSelectedChipTag(cgStatus);

            String deptTag = getSelectedChipTag(cgDept);
            if (deptTag != null) {
                filterDeptId = Long.parseLong(deptTag);
                for (Department d : cachedDepartments) {
                    if (d.getId().equals(filterDeptId)) {
                        filterDeptName = d.getName();
                        break;
                    }
                }
            } else {
                filterDeptId = null;
                filterDeptName = null;
            }

            filterPosition = getSelectedChipTag(cgRole);
            updateFilterInfoText();
            viewModel.filterEmployees(filterStatus, filterDeptId, filterPosition);
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void addChip(ChipGroup group, String label, String tag, boolean checked) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setTag(tag);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setChipStrokeWidth(2f);
        group.addView(chip);
    }

    private String getSelectedChipTag(ChipGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            if (chip.isChecked()) return (String) chip.getTag();
        }
        return null;
    }

    // ── FILTER LOGIC ──────────────────────────────────────────────

    private boolean hasActiveFilter() {
        return filterStatus != null || filterDeptId != null || filterPosition != null;
    }

    private void applyLocalFilter(List<Employee> fullList) {
        if (fullList == null) return;
        List<Employee> filtered = new ArrayList<>();
        for (Employee emp : fullList) {
            boolean statusOk = filterStatus == null || filterStatus.equals(emp.getStatusRaw());
            boolean deptOk = filterDeptId == null ||
                    (emp.getDepartmentId() != null && emp.getDepartmentId().equals(filterDeptId));
            boolean posOk = filterPosition == null || filterPosition.equals(emp.getPosition());
            if (statusOk && deptOk && posOk) filtered.add(emp);
        }
        adapter.setData(filtered);
        updateStats(filtered);
    }

    private void updateFilterInfoText() {
        if (!hasActiveFilter()) {
            tvFilterInfo.setVisibility(View.GONE);
            btnSort.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            return;
        }
        StringBuilder sb = new StringBuilder("Đang lọc: ");
        if (filterDeptName != null) sb.append("PB: ").append(filterDeptName).append("  ");
        if (filterStatus != null) {
            sb.append("ACTIVE".equals(filterStatus) ? "Đang làm" : "Nghỉ việc").append("  ");
        }
        if (filterPosition != null) sb.append("CV: ").append(filterPosition);
        tvFilterInfo.setText(sb.toString().trim());
        tvFilterInfo.setVisibility(View.VISIBLE);
        btnSort.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBBDEFB));
    }

    // ── EMPLOYEE DETAIL ───────────────────────────────────────────

    private void updateStats(List<Employee> list) {
        if (list == null) return;
        int total   = list.size();
        int working = (int) list.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatusRaw())).count();
        tvTotal.setText(String.valueOf(total));
        tvWorking.setText(String.valueOf(working));
        tvResigned.setText(String.valueOf(total - working));
    }

    private void showEmployeeDetail(Employee emp) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_employee_detail, null);

        ((TextView) view.findViewById(R.id.tvDialogAvatar)).setText(emp.getAvatarText());
        ((TextView) view.findViewById(R.id.tvDialogName)).setText(emp.getFullName());
        ((TextView) view.findViewById(R.id.tvDialogRole)).setText(emp.getRole());
        ((TextView) view.findViewById(R.id.tvDialogEmail)).setText(emp.getEmail());
        ((TextView) view.findViewById(R.id.tvDialogPhone)).setText(emp.getPhone());
        ((TextView) view.findViewById(R.id.tvDialogDept)).setText(emp.getDepartment());
        ((TextView) view.findViewById(R.id.tvDialogStatus)).setText(emp.getStatus());
        ((TextView) view.findViewById(R.id.tvDialogJoinDate)).setText(emp.getJoinDate());

        view.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> dialog.dismiss());

        // MỚI: Nút xem chi tiết cross-reference
        View btnViewDetail = view.findViewById(R.id.btnViewDetail);
        if (btnViewDetail != null) {
            btnViewDetail.setVisibility(View.VISIBLE);
            btnViewDetail.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, EmployeeDetailActivity.class);
                intent.putExtra("employeeId", emp.getId());
                startActivity(intent);
            });
        }

        if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
            MaterialButton btnEdit       = view.findViewById(R.id.btnEditEmployee);
            MaterialButton btnDelete     = view.findViewById(R.id.btnDeleteEmployee);
            MaterialButton btnReactivate = view.findViewById(R.id.btnReactivateEmployee);

            if (btnEdit != null) {
                btnEdit.setVisibility(View.VISIBLE);
                btnEdit.setOnClickListener(v -> {
                    dialog.dismiss();
                    // Sửa (truyền employeeId)
                    Intent intent = new Intent(this, AddEditEmployeeActivity.class);
                    intent.putExtra("employeeId", emp.getId());
                    startActivity(intent);
                });
            }

            boolean isResigned = "RESIGNED".equalsIgnoreCase(emp.getStatusRaw());

            if ("ADMIN".equals(role)) {
                if (isResigned) {
                    // NV đã nghỉ → hiện Khôi phục, ẩn Nghỉ việc
                    if (btnDelete != null) btnDelete.setVisibility(View.GONE);
                    if (btnReactivate != null) {
                        btnReactivate.setVisibility(View.VISIBLE);
                        btnReactivate.setOnClickListener(v -> {
                            dialog.dismiss();
                            confirmReactivate(emp);
                        });
                    }
                } else {
                    // NV đang làm → hiện Nghỉ việc, ẩn Khôi phục
                    if (btnReactivate != null) btnReactivate.setVisibility(View.GONE);
                    if (btnDelete != null) {
                        btnDelete.setVisibility(View.VISIBLE);
                        btnDelete.setOnClickListener(v -> {
                            dialog.dismiss();
                            confirmResign(emp);
                        });
                    }
                }
            }
        }

        dialog.setContentView(view);
        dialog.show();
    }

    /** Cho nhân viên nghỉ việc: ACTIVE → RESIGNED */
    private void confirmResign(Employee emp) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận nghỉ việc")
                .setMessage("Cho nhân viên " + emp.getFullName() + " nghỉ việc?")
                .setPositiveButton("Xác nhận", (d, w) -> {
                    apiService.resignEmployee(emp.getId())
                            .enqueue(new Callback<Void>() {
                                @Override public void onResponse(Call<Void> c, Response<Void> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(EmployeeActivity.this,
                                                "Đã cho nghỉ việc", Toast.LENGTH_SHORT).show();
                                        viewModel.loadEmployees();
                                    } else {
                                        ApiErrorHelper.show(EmployeeActivity.this, r, "Cho nghỉ việc thất bại");
                                    }
                                }
                                @Override public void onFailure(Call<Void> c, Throwable t) {
                                    Toast.makeText(EmployeeActivity.this,
                                            "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    /**
     * Khôi phục nhân viên: RESIGNED → ACTIVE
     * Dùng endpoint có sẵn: PUT api/admin/accounts/{userId}/status?status=ACTIVE
     * Đây là endpoint backend đã hoạt động (AccountManagement dùng để khóa/mở tài khoản)
     */
    private void confirmReactivate(Employee emp) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận khôi phục")
                .setMessage("Khôi phục nhân viên " + emp.getFullName() + " về trạng thái đang làm việc?")
                .setPositiveButton("Khôi phục", (d, w) -> {
                    // Gọi PUT /api/employees/{id}/reactivate
                    apiService.reactivateEmployee(emp.getId())
                            .enqueue(new Callback<Void>() {
                                @Override public void onResponse(Call<Void> c, Response<Void> r) {
                                    if (r.isSuccessful()) {
                                        Toast.makeText(EmployeeActivity.this,
                                                "Đã khôi phục nhân viên", Toast.LENGTH_SHORT).show();
                                        viewModel.loadEmployees();
                                    } else {
                                        ApiErrorHelper.show(EmployeeActivity.this, r, "Khôi phục nhân viên thất bại");
                                    }
                                }
                                @Override public void onFailure(Call<Void> c, Throwable t) {
                                    Toast.makeText(EmployeeActivity.this,
                                            "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadEmployees();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
    }
}