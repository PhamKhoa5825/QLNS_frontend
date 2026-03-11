package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.*;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EmployeeAdapter adapter;
    private ProgressBar progressBar;
    private EditText edtSearch;
    private FloatingActionButton fabAdd;

    // Thống kê
    private TextView tvTotal, tvWorking, tvResigned;

    private List<Employee> allEmployees = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private static final String PREF_NAME = "qlns_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_demo);

        initViews();
        setupSearch();
        loadEmployees();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        edtSearch   = findViewById(R.id.edtSearch);
        fabAdd      = findViewById(R.id.fabAddEmployee);

        // Ánh xạ các TextView thống kê (Phải thêm ID vào XML trước)
        tvTotal     = findViewById(R.id.tvTotal);
        tvWorking   = findViewById(R.id.tvWorking);
        tvResigned  = findViewById(R.id.tvResigned);

        recyclerView = findViewById(R.id.recyclerViewEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAdapter(new ArrayList<>(), this::showEmployeeDetail);
        recyclerView.setAdapter(adapter);

        // Phân quyền FAB
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String role = prefs.getString("role", "EMPLOYEE");
        if ("ADMIN".equals(role)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> showAddEmployeeDialog());
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private void setupSearch() {
        // 1. Xử lý Search realtime
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    String kw = s.toString().trim();
                    if (kw.isEmpty()) {
                        adapter.setData(allEmployees);
                    } else {
                        searchEmployee(kw);
                    }
                };
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });

        // 2. Xử lý Click vào icon Filter (bên phải EditText)
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

    private void loadEmployees() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getApiService().getEmployees().enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    allEmployees = response.body();
                    adapter.setData(allEmployees);
                    updateStatistics(allEmployees); // Cập nhật số liệu
                } else {
                    Toast.makeText(EmployeeActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(EmployeeActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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

        tvTotal.setText(String.valueOf(total));
        tvWorking.setText(String.valueOf(working));
        tvResigned.setText(String.valueOf(total - working));
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_employee_sort, null);

        ChipGroup cgRole = view.findViewById(R.id.cgRole);
        ChipGroup cgDepartment = view.findViewById(R.id.cgDepartment);
        ChipGroup cgStatus = view.findViewById(R.id.cgStatus);
        Button btnApply = view.findViewById(R.id.btnApply);
        Button btnReset = view.findViewById(R.id.btnReset);

        // Thêm trạng thái cố định
        addChip(cgStatus, "Đang làm việc", "ACTIVE");
        addChip(cgStatus, "Đã nghỉ việc", "RESIGNED");

        // Load Phòng ban từ API
        RetrofitClient.getApiService().getDepartments().enqueue(new Callback<List<Department>>() {
            @Override
            public void onResponse(Call<List<Department>> call, Response<List<Department>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Department dept : response.body())
                        addChipWithId(cgDepartment, dept.getName(), dept.getId());
                }
            }
            @Override public void onFailure(Call<List<Department>> call, Throwable t) {}
        });

        // Load Chức vụ từ list nhân viên hiện tại
        LinkedHashSet<String> positions = new LinkedHashSet<>();
        for (Employee emp : allEmployees) if (emp.getPosition() != null) positions.add(emp.getPosition());
        for (String pos : positions) addChip(cgRole, pos, pos);

        btnApply.setOnClickListener(v -> {
            String selectedStatus = getSelectedChipTag(cgStatus);
            Long selectedDeptId = getSelectedChipLongTag(cgDepartment);
            String selectedRole = getSelectedChipTag(cgRole);

            List<Employee> filtered = new ArrayList<>();
            for (Employee emp : allEmployees) {
                boolean statusOk = selectedStatus == null || selectedStatus.equals(emp.getStatusRaw());
                boolean deptOk = selectedDeptId == null || (emp.getDepartmentId() != null && emp.getDepartmentId().equals(selectedDeptId));
                boolean roleOk = selectedRole == null || selectedRole.equals(emp.getPosition());

                if (statusOk && deptOk && roleOk) filtered.add(emp);
            }
            adapter.setData(filtered);
            dialog.dismiss();
        });

        btnReset.setOnClickListener(v -> {
            adapter.setData(allEmployees);
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    // --- Helper Methods ---
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

    private void searchEmployee(String keyword) {
        RetrofitClient.getApiService().searchEmployees(keyword).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) adapter.setData(response.body());
            }
            @Override public void onFailure(Call<List<Employee>> call, Throwable t) {}
        });
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

        view.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
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