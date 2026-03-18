package com.example.myapplication.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.example.myapplication.R;
import com.example.myapplication.adapter.DepartmentAdapter;
import com.example.myapplication.model.Department;
import com.example.myapplication.viewmodel.DepartmentViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;

public class DepartmentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DepartmentAdapter adapter;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private DepartmentViewModel viewModel;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Status bar trong suốt, trùng màu toolbar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_department_demo);

        viewModel = new ViewModelProvider(this).get(DepartmentViewModel.class);
        role      = getSharedPreferences("qlns_pref", MODE_PRIVATE).getString("role", "EMPLOYEE");

        initViews();
        observeViewModel();
        viewModel.loadDepartments();
    }

    private void initViews() {
        findViewById(R.id.btnBackDept).setOnClickListener(v -> finish());
        progressBar = findViewById(R.id.progressBar);
        fabAdd      = findViewById(R.id.fabAddDept);

        recyclerView = findViewById(R.id.recyclerViewDept);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DepartmentAdapter(new ArrayList<>(), dept -> showDeptOptions(dept));
        recyclerView.setAdapter(adapter);

        if ("ADMIN".equals(role)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> showAddDeptDialog());
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private void observeViewModel() {
        viewModel.departments.observe(this, list -> {
            if (list != null) adapter.setData(list);
        });
        viewModel.isLoading.observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
        viewModel.errorMessage.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
        viewModel.successMessage.observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.loadDepartments();
            }
        });
    }

    private void showDeptOptions(Department dept) {
        if ("ADMIN".equals(role)) {
            String[] options = {"Xem nhân viên", "Sửa phòng ban", "Xóa phòng ban"};
            new AlertDialog.Builder(this)
                    .setTitle(dept.getName())
                    .setItems(options, (d, which) -> {
                        switch (which) {
                            case 0: navigateToEmployeeWithFilter(dept); break;
                            case 1: showEditDeptDialog(dept); break;
                            case 2: confirmDelete(dept); break;
                        }
                    })
                    .show();
        } else {
            navigateToEmployeeWithFilter(dept);
        }
    }

    /**
     * Chuyển sang EmployeeActivity với filter theo phòng ban.
     * EmployeeActivity nhận filterDeptId/filterDeptName qua Intent
     * và tự động lọc + hiển thị thông tin bộ lọc.
     */
    private void navigateToEmployeeWithFilter(Department dept) {
        Intent intent = new Intent(this, EmployeeActivity.class);
        intent.putExtra("filterDeptId", dept.getId());
        intent.putExtra("filterDeptName", dept.getName());
        startActivity(intent);
    }

    private void confirmDelete(Department dept) {
        new AlertDialog.Builder(this)
                .setMessage("Xóa phòng ban \"" + dept.getName() + "\"?\nNhân viên trong phòng ban sẽ không còn thuộc phòng ban này.")
                .setPositiveButton("Xóa", (d, w) -> viewModel.deleteDepartment(dept.getId()))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showAddDeptDialog() {
        showDeptFormDialog(null, null, null);
    }

    private void showEditDeptDialog(Department dept) {
        showDeptFormDialog(dept.getId(), dept.getName(), dept.getDescription());
    }

    private void showDeptFormDialog(Long id, String oldName, String oldDesc) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_department, null);
        TextInputEditText etName = view.findViewById(R.id.etDeptName);
        TextInputEditText etDesc = view.findViewById(R.id.etDeptDesc);
        MaterialButton btnSave   = view.findViewById(R.id.btnSave);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);

        boolean isEdit = (id != null);
        if (isEdit) {
            etName.setText(oldName);
            etDesc.setText(oldDesc);
            btnSave.setText("Cập nhật");
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            if (name.isEmpty()) { etName.setError("Không được để trống"); return; }
            if (isEdit) viewModel.updateDepartment(id, name, desc);
            else viewModel.createDepartment(name, desc);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}