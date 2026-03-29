package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.DepartmentAdapter;
import com.example.myapplication.model.entity.Department;
import com.example.myapplication.network.ApiErrorHelper;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DepartmentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DepartmentAdapter adapter;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;
    private ApiService apiService;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Status bar trong suốt, trùng màu toolbar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_department);

        apiService = RetrofitClient.getClient().create(ApiService.class);
        role       = getSharedPreferences("qlns_pref", MODE_PRIVATE).getString("role", "EMPLOYEE");

        initViews();
        loadDepartments();
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

    // ── LOAD ─────────────────────────────────────────────────────

    private void loadDepartments() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getDepartments().enqueue(new Callback<List<Department>>() {
            @Override
            public void onResponse(Call<List<Department>> c, Response<List<Department>> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    adapter.setData(r.body());
                } else {
                    Toast.makeText(DepartmentActivity.this,
                            ApiErrorHelper.parse(r, "Lỗi tải phòng ban"),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Department>> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DepartmentActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── OPTIONS ──────────────────────────────────────────────────

    /**
     * SỬA: Hiện dialog với các tuỳ chọn, thêm "Xem chi tiết" để mở DepartmentDetailActivity
     */
    private void showDeptOptions(Department dept) {
        if ("ADMIN".equals(role)) {
            String[] options = {"Xem chi tiết", "Xem nhân viên", "Sửa phòng ban", "Xóa phòng ban"};
            new AlertDialog.Builder(this)
                    .setTitle(dept.getName())
                    .setItems(options, (d, which) -> {
                        switch (which) {
                            case 0: navigateToDeptDetail(dept); break;
                            case 1: navigateToEmployeeWithFilter(dept); break;
                            case 2: showEditDeptDialog(dept); break;
                            case 3: confirmDelete(dept); break;
                        }
                    })
                    .show();
        } else {
            // Non-admin: trực tiếp mở trang chi tiết phòng ban
            navigateToDeptDetail(dept);
        }
    }

    /**
     * MỚI: Mở trang chi tiết phòng ban — hiển thị thông tin + danh sách nhân viên riêng
     */
    private void navigateToDeptDetail(Department dept) {
        Intent intent = new Intent(this, DepartmentDetailActivity.class);
        intent.putExtra("deptId", dept.getId());
        intent.putExtra("deptName", dept.getName());
        intent.putExtra("deptDescription", dept.getDescription());
        intent.putExtra("deptManagerName", dept.getManagerName());
        intent.putExtra("deptEmployeeCount", dept.getEmployeeCount());
        startActivity(intent);
    }

    /**
     * Chuyển sang EmployeeActivity với filter theo phòng ban.
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
                .setPositiveButton("Xóa", (d, w) -> deleteDepartment(dept.getId()))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void deleteDepartment(Long id) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.deleteDepartment(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                progressBar.setVisibility(View.GONE);
                if (r.isSuccessful()) {
                    Toast.makeText(DepartmentActivity.this, "Đã xóa phòng ban", Toast.LENGTH_SHORT).show();
                    loadDepartments();
                } else {
                    Toast.makeText(DepartmentActivity.this,
                            ApiErrorHelper.parse(r, "Không thể xóa phòng ban"),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DepartmentActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── FORM DIALOGS ─────────────────────────────────────────────

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

            Department dept = new Department();
            dept.setName(name);
            dept.setDescription(desc);

            if (isEdit) {
                apiService.updateDepartment(id, dept).enqueue(new Callback<Department>() {
                    @Override
                    public void onResponse(Call<Department> c, Response<Department> r) {
                        if (r.isSuccessful()) {
                            Toast.makeText(DepartmentActivity.this, "Đã cập nhật thông tin", Toast.LENGTH_SHORT).show();
                            loadDepartments();
                        } else {
                            Toast.makeText(DepartmentActivity.this,
                                    ApiErrorHelper.parse(r, "Không thể cập nhật phòng ban"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Department> c, Throwable t) {
                        Toast.makeText(DepartmentActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                apiService.createDepartment(dept).enqueue(new Callback<Department>() {
                    @Override
                    public void onResponse(Call<Department> c, Response<Department> r) {
                        if (r.isSuccessful()) {
                            Toast.makeText(DepartmentActivity.this, "Đã tạo phòng ban", Toast.LENGTH_SHORT).show();
                            loadDepartments();
                        } else {
                            Toast.makeText(DepartmentActivity.this,
                                    ApiErrorHelper.parse(r, "Không thể tạo phòng ban"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Department> c, Throwable t) {
                        Toast.makeText(DepartmentActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}