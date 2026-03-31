package com.example.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.EmployeeAdapter;
import com.example.myapplication.model.Department;
import com.example.myapplication.model.Employee;
import com.example.myapplication.repository.DepartmentRepository;
import com.example.myapplication.utils.TopBarHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class DepartmentDetailActivity extends AppCompatActivity {

    private DepartmentRepository repository;
    private Long deptId;
    private String deptName;

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department_detail);

        repository = new DepartmentRepository(this);

        // Get data from Intent
        deptId = getIntent().getLongExtra("deptId", -1);
        deptName = getIntent().getStringExtra("deptName");
        String deptDesc = getIntent().getStringExtra("deptDescription");
        String deptManager = getIntent().getStringExtra("deptManagerName");
        int deptEmpCount = getIntent().getIntExtra("deptEmployeeCount", 0);

        initViews(deptName, deptDesc, deptManager, deptEmpCount);
        loadEmployees();
    }

    private void initViews(String name, String desc, String manager, int count) {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (name != null) toolbar.setTitle(name);

        ((TextView) findViewById(R.id.tvDeptNameDetail)).setText(name != null ? name : "--");
        ((TextView) findViewById(R.id.tvDeptDescDetail)).setText(desc != null && !desc.isEmpty() ? desc : "Không có mô tả");
        ((TextView) findViewById(R.id.tvDeptManagerDetail)).setText("Trưởng phòng: " + (manager != null ? manager : "Chưa có"));
        ((TextView) findViewById(R.id.tvDeptEmpCountDetail)).setText(count + " nhân viên");

        rvEmployees = findViewById(R.id.rvEmployees);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmployeeAdapter(new ArrayList<>(), emp -> {
            Intent intent = new Intent(this, EmployeeDetailActivity.class);
            intent.putExtra("employeeId", emp.getId());
            startActivity(intent);
        });
        rvEmployees.setAdapter(adapter);
    }

    private void loadEmployees() {
        if (deptId == -1) return;

        progressBar.setVisibility(View.VISIBLE);
        repository.getEmployeesByDepartment(deptId, new DepartmentRepository.RepositoryCallback<List<Employee>>() {
            @Override
            public void onSuccess(List<Employee> data) {
                progressBar.setVisibility(View.GONE);
                if (data.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvEmployees.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvEmployees.setVisibility(View.VISIBLE);
                    adapter.setData(data);
                    ((TextView) findViewById(R.id.tvDeptEmpCountDetail)).setText(data.size() + " nhân viên");
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DepartmentDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
