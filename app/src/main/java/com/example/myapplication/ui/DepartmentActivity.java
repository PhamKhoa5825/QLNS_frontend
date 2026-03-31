package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.DepartmentAdapter;
import com.example.myapplication.model.Department;
import com.example.myapplication.utils.BottomNavHelper;
import com.example.myapplication.utils.SharedPrefsManager;
import com.example.myapplication.utils.TopBarHelper;
import com.example.myapplication.viewmodel.DepartmentViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class DepartmentActivity extends AppCompatActivity {

    private DepartmentViewModel viewModel;
    private DepartmentAdapter adapter;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_demo);

        userRole = SharedPrefsManager.getInstance(this).getRole();
        viewModel = new ViewModelProvider(this).get(DepartmentViewModel.class);

        initViews();
        setupObservers();
        viewModel.loadDepartments();
    }

    private void initViews() {
        TopBarHelper.setupTopBar(this);
        BottomNavHelper.setupBottomNav(this, -1);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewDept);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new DepartmentAdapter(new ArrayList<>(), this::showDepartmentOptions);
        recyclerView.setAdapter(adapter);

        setupSearch();
        setupFAB();
    }

    private void setupSearch() {
        EditText edtSearch = findViewById(R.id.edtSearchDept);
        if (edtSearch == null) return;
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchDepartments(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFAB() {
        View fab = findViewById(R.id.fabAddDept);
        if (fab == null) return;
        if ("ADMIN".equals(userRole)) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> showDepartmentForm(null));
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    private void setupObservers() {
        viewModel.departments.observe(this, depts -> adapter.setData(depts));
        
        viewModel.isLoading.observe(this, isLoading -> {
            View pb = findViewById(R.id.progressBar);
            if (pb != null) pb.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.errorMessage.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.successMessage.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDepartmentOptions(Department dept) {
        DepartmentOptionsBottomSheet bottomSheet = DepartmentOptionsBottomSheet.newInstance(dept, new DepartmentOptionsBottomSheet.OnOptionSelectedListener() {
            @Override
            public void onViewDetail(Department d) {
                navigateToDetail(d);
            }

            @Override
            public void onEdit(Department d) {
                showDepartmentForm(d);
            }

            @Override
            public void onDelete(Department d) {
                confirmDelete(d);
            }
        });
        bottomSheet.show(getSupportFragmentManager(), "department_options");
    }

    private void navigateToDetail(Department dept) {
        Intent intent = new Intent(this, DepartmentDetailActivity.class);
        intent.putExtra("deptId", dept.getId());
        intent.putExtra("deptName", dept.getName());
        intent.putExtra("deptDescription", dept.getDescription());
        intent.putExtra("deptManagerName", dept.getManagerName());
        intent.putExtra("deptEmployeeCount", dept.getEmployeeCount());
        startActivity(intent);
    }

    private void showDepartmentForm(Department dept) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_department_form, null);
        TextInputEditText etName = view.findViewById(R.id.etDeptName);
        TextInputEditText etDesc = view.findViewById(R.id.etDeptDesc);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);

        boolean isEdit = (dept != null);
        if (isEdit) {
            if (tvTitle != null) tvTitle.setText("Chỉnh sửa phòng ban");
            if (etName != null) etName.setText(dept.getName());
            if (etDesc != null) etDesc.setText(dept.getDescription());
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(view);

        AlertDialog dialog = builder.create();

        View btnSave = view.findViewById(R.id.btnSave);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
                String desc = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                
                if (name.isEmpty()) {
                    if (etName != null) etName.setError("Tên không được để trống");
                    return;
                }

                Department newDept = new Department();
                newDept.setName(name);
                newDept.setDescription(desc);

                if (isEdit) {
                    viewModel.updateDepartment(dept.getId(), newDept);
                } else {
                    viewModel.addDepartment(newDept);
                }
                dialog.dismiss();
            });
        }

        View btnCancel = view.findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        
        dialog.show();
    }

    private void confirmDelete(Department dept) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Xóa phòng ban")
                .setMessage("Bạn có chắc chắn muốn xóa phòng ban \"" + dept.getName() + "\"?")
                .setPositiveButton("Xóa", (d, w) -> viewModel.deleteDepartment(dept.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }
}
