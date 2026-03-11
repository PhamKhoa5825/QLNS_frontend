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

    private EmployeeViewModel viewModel;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private static final String PREF_NAME = "qlns_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_demo);

        viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);

        initViews();
        setupSearch();
        observeViewModel();

        viewModel.loadEmployees();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressBar);
        edtSearch   = findViewById(R.id.edtSearch);
        fabAdd      = findViewById(R.id.fabAddEmployee);
        tvTotal     = findViewById(R.id.tvTotal);
        tvWorking   = findViewById(R.id.tvWorking);
        tvResigned  = findViewById(R.id.tvResigned);

        recyclerView = findViewById(R.id.recyclerViewEmployee);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EmployeeAdapter(new ArrayList<>(), this::showEmployeeDetail);
        recyclerView.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String role = prefs.getString("role", "EMPLOYEE");
        if ("ADMIN".equals(role)) {
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

        addChip(cgStatus, "Đang làm việc", "ACTIVE");
        addChip(cgStatus, "Đã nghỉ việc", "RESIGNED");

        viewModel.departments.observe(this, departments -> {
            cgDepartment.removeAllViews();
            if (departments != null) {
                for (Department dept : departments) {
                    addChipWithId(cgDepartment, dept.getName(), dept.getId());
                }
            }
        });
        viewModel.loadDepartments();

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
            viewModel.loadEmployees();
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
